package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.services.ValidationException;
import com.example.services.ValidationService;

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
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;

    @Inject
    public ReactiveWithExceptionsPaymentProcessor(
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

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> {
                ValidationResult result = merchantValidationService.validate(request);
                if (!result.success()) {
                    throw new ValidationException(result);
                }
                return result;
            });

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<ValidationResult> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    throw new ValidationException(cardResult);
                }

                // B2: Nested parallel validations using SAME exception-based approach as structured concurrency
                List<CompletableFuture<ValidationResult>> validationFutures = cardValidations.stream()
                    .map(service -> CompletableFuture.supplyAsync(() -> {
                        ValidationResult result = service.validate(request);
                        if (!result.success()) {
                            throw new ValidationException(result);
                        }
                        return result;
                    }))
                    .toList();

                // CompletableFuture.allOf() waits for ALL tasks to complete (or fail)
                // This is the KEY difference from StructuredTaskScope which cancels immediately
                CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).join();

                // If we get here, all validations passed
                return ValidationResult.success("All consumer validations passed");
            });

        // Group top-level validations
        List<CompletableFuture<ValidationResult>> topLevelValidations = List.of(merchantValidation, consumerValidation);

        // Wait for BOTH parallel paths (merchant AND consumer)
        return CompletableFuture.allOf(topLevelValidations.toArray(new CompletableFuture[0]))
            .thenCompose(_ -> {
                // Check all validation results uniformly
                topLevelValidations.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> !r.success())
                    .findFirst()
                    .ifPresent(failure -> {
                        throw new ValidationException(failure);
                    });

                // Step 3: Transfer amount if all validations passed
                logger.debug("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request));
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

}
