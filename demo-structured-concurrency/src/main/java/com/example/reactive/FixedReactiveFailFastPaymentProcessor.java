package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.PinValidationService;
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
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;

    public FixedReactiveFailFastPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Validate card first (sequential)
        return CompletableFuture
            .supplyAsync(() -> cardValidationService.validate(request.cardNumber()))
            .thenCompose(cardResult -> {
                if (!cardResult.success()) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    System.out.println("❌ FIXED REACTIVE FAIL-FAST transaction failed: " + cardResult.message() +
                                     " (in " + processingTime + "ms)");
                    return CompletableFuture.completedFuture(TransactionResult.failure(cardResult.message(), processingTime));
                }

                System.out.println("✅ Card validation passed, proceeding with parallel validations...");

                // Step 2: Parallel validations with fail-fast coordination
                AtomicBoolean hasFailed = new AtomicBoolean(false);
                AtomicReference<String> failureReason = new AtomicReference<>();
                List<CompletableFuture<ValidationResult>> futures = new ArrayList<>();

                // Balance validation
                CompletableFuture<ValidationResult> balanceFuture = CompletableFuture
                    .supplyAsync(() -> {
                        if (hasFailed.get()) {
                            throw new CompletionException(new RuntimeException("Cancelled due to earlier failure"));
                        }
                        ValidationResult result = balanceService.validate(request.cardNumber(), request.amount());
                        if (!result.success() && hasFailed.compareAndSet(false, true)) {
                            failureReason.set(result.message());
                            futures.forEach(f -> f.cancel(true));
                            throw new CompletionException(new RuntimeException(result.message()));
                        }
                        return result;
                    });

                // Expiration validation
                CompletableFuture<ValidationResult> expirationFuture = CompletableFuture
                    .supplyAsync(() -> {
                        if (hasFailed.get()) {
                            throw new CompletionException(new RuntimeException("Cancelled due to earlier failure"));
                        }
                        ValidationResult result = expirationService.validate(request.cardNumber(), request.expirationDate());
                        if (!result.success() && hasFailed.compareAndSet(false, true)) {
                            failureReason.set(result.message());
                            futures.forEach(f -> f.cancel(true));
                            throw new CompletionException(new RuntimeException(result.message()));
                        }
                        return result;
                    });

                // PIN validation
                CompletableFuture<ValidationResult> pinFuture = CompletableFuture
                    .supplyAsync(() -> {
                        if (hasFailed.get()) {
                            throw new CompletionException(new RuntimeException("Cancelled due to earlier failure"));
                        }
                        ValidationResult result = pinValidationService.validate(request.cardNumber(), request.pin());
                        if (!result.success() && hasFailed.compareAndSet(false, true)) {
                            failureReason.set(result.message());
                            futures.forEach(f -> f.cancel(true));
                            throw new CompletionException(new RuntimeException(result.message()));
                        }
                        return result;
                    });

                futures.addAll(List.of(balanceFuture, expirationFuture, pinFuture));

                // Wait for all parallel validations or first failure
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> {
                        // Step 3: All validations passed, proceed with debit
                        System.out.println("✅ All validations passed, proceeding with debit...");
                        return CompletableFuture.supplyAsync(() ->
                            balanceService.debit(request.cardNumber(), request.amount()));
                    })
                    .thenApply(debitResult -> {
                        if (!debitResult.success()) {
                            long processingTime = System.currentTimeMillis() - startTime;
                            System.out.println("❌ FIXED REACTIVE FAIL-FAST transaction failed: " + debitResult.message() +
                                             " (in " + processingTime + "ms)");
                            return TransactionResult.failure(debitResult.message(), processingTime);
                        }

                        long processingTime = System.currentTimeMillis() - startTime;
                        String transactionId = UUID.randomUUID().toString();

                        System.out.println("✅ FIXED REACTIVE FAIL-FAST transaction completed: " + transactionId +
                                         " (in " + processingTime + "ms)");

                        return TransactionResult.success(transactionId, request.amount(), processingTime);
                    })
                    .exceptionally(throwable -> {
                        long processingTime = System.currentTimeMillis() - startTime;
                        String reason = failureReason.get();

                        if (reason == null) {
                            if (throwable.getCause() != null) {
                                reason = throwable.getCause().getMessage();
                            } else {
                                reason = "Processing error: " + throwable.getMessage();
                            }
                        }

                        System.out.println("❌ FIXED REACTIVE FAIL-FAST transaction failed: " + reason +
                                         " (in " + processingTime + "ms)");
                        System.out.println("   ⚡ Attempted to cancel remaining validations");

                        return TransactionResult.failure(reason, processingTime);
                    });
            });
    }

}
