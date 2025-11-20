package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
public class ReactiveWithExceptionsPaymentProcessor implements ReactivePaymentProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final MerchantValidationService merchantValidationService;
    private final PinValidationService pinValidationService;

    public ReactiveWithExceptionsPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.merchantValidationService = new MerchantValidationService();
        this.pinValidationService = new PinValidationService();
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> {
                ValidationResult result = merchantValidationService.validate(request);
                if (!result.success()) {
                    throw new RuntimeException(result.message());
                }
                return result;
            });

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<ValidationResult> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    throw new RuntimeException(cardResult.message());
                }

                // B2: Nested parallel validations using SAME exception-based approach as structured concurrency
                CompletableFuture<ValidationResult> balanceValidation = CompletableFuture
                    .supplyAsync(() -> {
                        ValidationResult result = balanceService.validate(request);
                        if (!result.success()) {
                            throw new RuntimeException(result.message());
                        }
                        return result;
                    });

                CompletableFuture<ValidationResult> expirationValidation = CompletableFuture
                    .supplyAsync(() -> {
                        ValidationResult result = expirationService.validate(request);
                        if (!result.success()) {
                            throw new RuntimeException(result.message());
                        }
                        return result;
                    });

                CompletableFuture<ValidationResult> pinValidation = CompletableFuture
                    .supplyAsync(() -> {
                        ValidationResult result = pinValidationService.validate(request);
                        if (!result.success()) {
                            throw new RuntimeException(result.message());
                        }
                        return result;
                    });

                // CompletableFuture.allOf() waits for ALL tasks to complete (or fail)
                // This is the KEY difference from StructuredTaskScope which cancels immediately
                CompletableFuture.allOf(balanceValidation, expirationValidation, pinValidation).join();

                // If we get here, all validations passed
                return ValidationResult.success("All consumer validations passed");
            });

        // Wait for BOTH parallel paths (merchant AND consumer)
        return CompletableFuture.allOf(merchantValidation, consumerValidation)
            .thenCompose(_ -> {
                // Check results from both paths
                ValidationResult merchantResult = merchantValidation.join();
                ValidationResult consumerResult = consumerValidation.join();

                if (!merchantResult.success()) {
                    throw new RuntimeException(merchantResult.message());
                }
                if (!consumerResult.success()) {
                    throw new RuntimeException(consumerResult.message());
                }

                // Step 3: Transfer amount if all validations passed
                System.out.println("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request));
            })
            .thenApply(_ -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String transactionId = UUID.randomUUID().toString();

                System.out.println("✅ REACTIVE WITH EXCEPTIONS transaction completed: " + transactionId +
                                 " (in " + processingTime + "ms)");

                return TransactionResult.success(transactionId, request.amount(), processingTime);
            })
            .exceptionally(throwable -> {
                balanceService.releaseAmount(request);
                long processingTime = System.currentTimeMillis() - startTime;

                // Extract the original validation exception
                String failureReason = "Processing error";
                if (throwable instanceof CompletionException ce && ce.getCause() != null) {
                    failureReason = ce.getCause().getMessage();
                } else if (throwable instanceof RuntimeException re) {
                    failureReason = re.getMessage();
                }

                System.out.println("❌ REACTIVE WITH EXCEPTIONS transaction failed: " + failureReason +
                                 " (in " + processingTime + "ms)");
                System.out.println("   ⚠️  All other validations still completed - no automatic cancellation");

                return TransactionResult.failure(failureReason, processingTime);
            });
    }

}
