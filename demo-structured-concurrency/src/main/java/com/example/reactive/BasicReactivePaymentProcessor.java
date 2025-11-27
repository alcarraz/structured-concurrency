package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.services.ValidationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class BasicReactivePaymentProcessor implements ReactivePaymentProcessor {
    private static final Logger logger = LogManager.getLogger(BasicReactivePaymentProcessor.class);

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;
    
    public BasicReactivePaymentProcessor() {
        this(new BalanceService(), new CardValidationService(), new ExpirationService(), new PinValidationService(), new MerchantValidationService());
    }
    @Inject
    public BasicReactivePaymentProcessor(
            BalanceService balanceService,
            CardValidationService cardValidationService,
            ExpirationService expirationService,
            PinValidationService pinValidationService,
            MerchantValidationService merchantValidationService) {
        this.balanceService = balanceService;
        this.cardValidationService = cardValidationService;
        this.merchantValidationService = merchantValidationService;
        this.cardValidations = List.of(expirationService, pinValidationService, balanceService);
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Run merchant validation in parallel with ENTIRE consumer validation flow

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> merchantValidationService.validate(request));

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<ValidationResult> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first (sequential within consumer path)
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    return cardResult;
                }

                // B2: Nested parallel validations (balance, PIN, expiration)
                List<CompletableFuture<ValidationResult>> validationFutures = cardValidations.stream()
                    .map(service -> CompletableFuture.supplyAsync(() -> service.validate(request)))
                    .toList();

                // Wait for all nested validations
                CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).join();

                // Check nested validation results
                return validationFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> !r.success())
                    .findFirst()
                    .orElse(ValidationResult.success("All card validations passed"));

            });

        // Group top-level validations
        List<CompletableFuture<ValidationResult>> topLevelValidations = List.of(merchantValidation, consumerValidation);

        // Wait for BOTH parallel paths (merchant + complete consumer flow)
        return CompletableFuture.allOf(topLevelValidations.toArray(new CompletableFuture[0]))
            .thenCompose(_ -> {
                // Check all validation results uniformly
                Optional<ValidationResult> failure = topLevelValidations.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> !r.success())
                    .findFirst();

                if (failure.isPresent()) {
                    balanceService.releaseAmount(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    logger.info("❌ REACTIVE transaction failed: {} (in {}ms)",
                               failure.get().message(), processingTime);
                    return CompletableFuture.completedFuture(
                        TransactionResult.failure(failure.get().message(), processingTime));
                }

                // Step 2: Transfer amount if both paths succeeded
                return CompletableFuture
                    .runAsync(() -> balanceService.transfer(request))
                    .thenApply(_ -> {
                        long processingTime = System.currentTimeMillis() - startTime;

                        String transactionId = UUID.randomUUID().toString();
                        logger.info("✅ REACTIVE transaction completed: {} (in {}ms)",
                                   transactionId, processingTime);
                        return TransactionResult.success(transactionId, request.amount(), processingTime);
                    });
            })
            .exceptionally(throwable -> {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("💥 REACTIVE transaction error: {} (in {}ms)",
                           throwable.getMessage(), processingTime);
                return TransactionResult.failure("Processing error: " + throwable.getMessage(), processingTime);
            });
    }

}
