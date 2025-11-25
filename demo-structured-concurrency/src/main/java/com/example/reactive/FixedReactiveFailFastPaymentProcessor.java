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
import java.util.concurrent.CompletionException;

/**
 * "Fixed" reactive implementation that attempts to fail fast using CompletableFuture
 * and exception handling. This shows how one COULD try to fix reactive programming
 * to achieve fail-fast behavior, but demonstrates the complexity required.
 * <p>
 * Note: Even this "fix" is significantly more complex than structured concurrency's
 * built-in fail-fast behavior, requiring manual cancellation logic and
 * careful exception handling that is prone to race conditions.
 */
public class FixedReactiveFailFastPaymentProcessor implements ReactivePaymentProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;

    public FixedReactiveFailFastPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
        this.cardValidations = List.of(expirationService, pinValidationService, balanceService);
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // PATH A: Merchant validation (runs independently with fail-fast)
        CompletableFuture<ValidationResult> merchantValidation = CompletableFuture
            .supplyAsync(() -> {
                ValidationResult result = merchantValidationService.validate(request);
                if (!result.success()) {
                    throw new ValidationException(result);
                }
                return result;
            });

        // PATH B: Consumer validation (card → nested parallel validations with fail-fast)
        CompletableFuture<ValidationResult> consumerValidation = CompletableFuture
            .supplyAsync(() -> {
                // B1: Validate card first
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    throw new ValidationException(cardResult);
                }

                // B2: Nested parallel validations with TRUE fail-fast coordination
                // Create a future that completes as soon as we know the final outcome
                CompletableFuture<ValidationResult> failFast = new CompletableFuture<>();

                // Create validation futures that notify the failFast future
                List<CompletableFuture<ValidationResult>> futures = cardValidations.stream()
                    .map(service -> CompletableFuture
                        .supplyAsync(() -> {
                            ValidationResult result = service.validate(request);
                            // On failure, complete failFast immediately (first one wins)
                            if (!result.success() && !failFast.isDone()) {
                                failFast.completeExceptionally(new ValidationException(result));
                            }
                            return result;
                        }))
                    .toList();

                // When all validations complete successfully, complete failFast with success
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        if (!failFast.isDone()) {
                            failFast.complete(ValidationResult.success("All consumer validations passed"));
                        }
                    });

                // Return the failFast future - it completes as soon as we know the outcome
                // (first failure OR all successes)
                try {
                    return failFast.join();
                } catch (CompletionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ValidationException ve) {
                        throw new RuntimeException(ve.getResult().message());
                    }
                    throw new RuntimeException("Consumer validation failed");
                }
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
                System.out.println("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request));
            })
            .thenApply(_ -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String transactionId = UUID.randomUUID().toString();

                System.out.println("✅ FIXED REACTIVE FAIL-FAST transaction completed: " + transactionId +
                                 " (in " + processingTime + "ms)");

                return TransactionResult.success(transactionId, request.amount(), processingTime);
            })
            .exceptionally(throwable -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String reason = throwable.getMessage();

                System.out.println("❌ FIXED REACTIVE FAIL-FAST transaction failed: " + reason +
                                 " (in " + processingTime + "ms)");
                System.out.println("   ⚡ Attempted to cancel remaining validations");

                return TransactionResult.failure(reason, processingTime);
            });
    }

}
