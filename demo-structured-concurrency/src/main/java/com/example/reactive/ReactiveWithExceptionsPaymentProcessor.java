package com.example.reactive;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardAwareValidationService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.services.ValidationException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reactive implementation that uses exceptions (like structured concurrency does)
 * but still DOES NOT fail fast naturally. This demonstrates that the problem
 * with traditional reactive programming isn't just about ValidationResult vs exceptions,
 * but about the fundamental architecture.
 * <p>
 * Even when using the same exception-based validation as structured concurrency,
 * CompletableFuture waits for ALL tasks to complete before processing results.
 * This is the key difference from StructuredTaskScope which cancels immediately.
 */
@ApplicationScoped
public class ReactiveWithExceptionsPaymentProcessor implements ReactivePaymentProcessor {
    private static final Logger logger = LogManager.getLogger(ReactiveWithExceptionsPaymentProcessor.class);

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    @Inject
    public ReactiveWithExceptionsPaymentProcessor(
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

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> switch (merchantValidationService.validate(request)) {
                case ValidationResult.Failure(String m) -> throw new ValidationException(m);
                case ValidationResult.Success success -> success; 
            });

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<Card> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first
                CardValidationResult cardResult = cardValidationService.validate(request);

                // Pattern match - throw exception on failure
                Card card = switch (cardResult) {
                    case CardValidationResult.Success(Card c) -> c;
                    case CardValidationResult.Failure(String msg) ->
                        throw new ValidationException(msg);
                };

                // B2: Nested parallel validations using SAME exception-based approach as structured concurrency (with Card)
                List<CompletableFuture<ValidationResult>> validationFutures = List.of(
                        validationTask(expirationService, request, card),
                        validationTask(pinValidationService, request, card),
                        validationTask(balanceService, request, card)
                );

                // CompletableFuture.allOf() waits for ALL tasks to complete (or fail)
                // This is the KEY difference from StructuredTaskScope which cancels immediately
                CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).join();

                // If we get here, all validations passed - return the Card
                return card;
            });

        // Wait for BOTH parallel paths (merchant AND consumer)
        return CompletableFuture.allOf(merchantValidation, consumerValidation)
            .thenCompose(_ -> {
                // If we get here, all validations passed (exceptions would have been thrown otherwise)
                // Extract the card for transfer
                Card card = consumerValidation.join();

                // Step 3: Transfer amount if all validations passed
                logger.debug("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request, card));
            })
            .thenApply(_ -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String transactionId = UUID.randomUUID().toString();

                logger.info("✅ REACTIVE WITH EXCEPTIONS transaction completed: {} (in {}ms)",
                           transactionId, processingTime);

                return TransactionResult.success(transactionId, request.amount(), processingTime);
            })
            .exceptionally(throwable -> {
                balanceService.releaseAmount(request);
                long processingTime = System.currentTimeMillis() - startTime;

                String failureReason = throwable.getMessage();

                logger.info("❌ REACTIVE WITH EXCEPTIONS transaction failed: {} (in {}ms)",
                           failureReason, processingTime);
                logger.debug("   ⚠️  All other validations still completed - no automatic cancellation");

                return TransactionResult.failure(failureReason, processingTime);
            });
    }

    private CompletableFuture<ValidationResult> validationTask(CardAwareValidationService service, TransactionRequest request, Card card) {
        return CompletableFuture.supplyAsync(() -> switch(service.validate(request, card)){
            case ValidationResult.Success success -> success;
            case ValidationResult.Failure(String msg) -> throw new ValidationException(msg);
        });
    }
}
