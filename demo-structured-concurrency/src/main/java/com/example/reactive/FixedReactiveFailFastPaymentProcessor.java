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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

                // B2: Nested parallel validations with fail-fast coordination
                AtomicBoolean hasFailed = new AtomicBoolean(false);
                AtomicReference<String> failureReason = new AtomicReference<>();
                List<CompletableFuture<ValidationResult>> futures = new ArrayList<>();

                // Create validation futures for all card validations
                for (ValidationService service : cardValidations) {
                    CompletableFuture<ValidationResult> future = CompletableFuture
                        .supplyAsync(() -> {
                            if (hasFailed.get()) {
                                throw new RuntimeException("Cancelled due to earlier failure");
                            }
                            ValidationResult result = service.validate(request);
                            if (!result.success() && hasFailed.compareAndSet(false, true)) {
                                failureReason.set(result.message());
                                futures.forEach(f -> f.cancel(true));
                                throw new ValidationException(result);
                            }
                            return result;
                        });
                    futures.add(future);
                }

                // Wait for all parallel validations or first failure
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    return ValidationResult.success("All consumer validations passed");
                } catch (CompletionException e) {
                    String reason = failureReason.get();
                    if (reason == null) {
                        reason = e.getMessage();
                    }
                    throw new RuntimeException(reason != null ? reason : "Consumer validation failed");
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
