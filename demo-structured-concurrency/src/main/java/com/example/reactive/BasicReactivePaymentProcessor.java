package com.example.reactive;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class BasicReactivePaymentProcessor implements ReactivePaymentProcessor {
    private static final Logger logger = LogManager.getLogger(BasicReactivePaymentProcessor.class);

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    @Inject
    public BasicReactivePaymentProcessor(
            BalanceService balanceService,
            CardValidationService cardValidationService,
            ExpirationService expirationService,
            PinValidationService pinValidationService,
            MerchantValidationService merchantValidationService) {
        this.balanceService = balanceService;
        this.cardValidationService = cardValidationService;
        this.expirationService = expirationService;
        this.pinValidationService = pinValidationService;
        this.merchantValidationService = merchantValidationService;
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Run merchant validation in parallel with ENTIRE consumer validation flow

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> merchantValidationService.validate(request));

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<CardValidationResult> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first (sequential within consumer path)
                CardValidationResult cardResult = cardValidationService.validate(request);

                // Pattern match on the result
                return switch (cardResult) {
                    case CardValidationResult.Success(Card card) -> {
                        // B2: Nested parallel validations (with Card passed to each)
                        List<CompletableFuture<ValidationResult>> validationFutures = List.of(
                            CompletableFuture.supplyAsync(() -> expirationService.validate(request, card)),
                            CompletableFuture.supplyAsync(() -> pinValidationService.validate(request, card)),
                            CompletableFuture.supplyAsync(() -> balanceService.validate(request, card))
                        );

                        // Wait for all nested validations
                        CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).join();

                        // Check nested validation results
                        yield validationFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(ValidationResult.Failure.class::isInstance)
                            .map(ValidationResult.Failure.class::cast)
                            .findFirst()
                            .map(r -> CardValidationResult.failure(r.message()))
                            .orElse(CardValidationResult.success(card));
                    }
                    case CardValidationResult.Failure failure -> failure;
                };
            });

        // Wait for BOTH parallel paths (merchant + complete consumer flow)
        return CompletableFuture.allOf(merchantValidation, consumerValidation)
            .thenCompose(_ -> {
                // Check all validation results uniformly

                return Stream.of(merchantValidation, consumerValidation)
                        .map(CompletableFuture::join)
                        .filter(CardValidationResult::isFailure)
                        .findFirst()
                        .map(failure -> {
                            balanceService.releaseAmount(request);
                            long processingTime = System.currentTimeMillis() - startTime;
                            logger.info("❌ REACTIVE transaction failed: {} (in {}ms)",
                                    failure.message(), processingTime);
                            return CompletableFuture.completedFuture(
                                    TransactionResult.failure(failure.message(), processingTime));
                        })
                        .orElseGet(() ->
                                // Step 2: Transfer amount if both paths succeeded
                                CompletableFuture
                                        .runAsync(() -> balanceService.transfer(request))
                                        .thenApply(_ -> {
                                            long processingTime = System.currentTimeMillis() - startTime;

                                            String transactionId = UUID.randomUUID().toString();
                                            logger.info("✅ REACTIVE transaction completed: {} (in {}ms)",
                                                    transactionId, processingTime);
                                            return TransactionResult.success(transactionId, request.amount(), processingTime);
                                        })                            
                        );
            })
            .exceptionally(throwable -> {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("💥 REACTIVE transaction error: {} (in {}ms)",
                           throwable.getMessage(), processingTime);
                return TransactionResult.failure("Processing error: " + throwable.getMessage(), processingTime);
            });
    }

}
