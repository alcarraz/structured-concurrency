package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/**
 * Fail-fast Structured Concurrency implementation with parallel merchant and consumer validation,
 * followed by nested parallel card validations.
 * <p>
 * Flow:
 * 1. Parallel: Validate Merchant AND Validate Card (fail-fast)
 * 2. Parallel (if card OK): Validate Balance, PIN, Expiration (fail-fast)
 * 3. Transfer (if all OK)
 * <p>
 * This demonstrates structured concurrency's automatic fail-fast and cancellation
 * capabilities - when any validation fails, remaining tasks are automatically cancelled.
 */
public class FailFastStructuredPaymentProcessor implements StructuredProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    public FailFastStructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 Starting FAIL-FAST STRUCTURED transaction processing for merchant " + request.merchant());

        try {
            // Step 1: Parallel - Validate Merchant AND Consumer (Card) with fail-fast
            try (var globalScope = StructuredTaskScope.open()) {

                // Fork merchant validation
                var merchantTask = globalScope.fork(() -> {
                    ValidationResult result = merchantValidationService.validate(request);
                    if (!result.success()) {
                        throw new RuntimeException(result.message());
                    }
                    return result;
                });

                // Fork consumer validation path (card + nested parallel validations)
                var consumerTask = globalScope.fork(() -> {
                    // First validate card
                    ValidationResult cardResult = cardValidationService.validate(request);
                    if (!cardResult.success()) {
                        throw new RuntimeException(cardResult.message());
                    }

                    // Step 2: Parallel - Validate Balance, PIN, and Expiration with fail-fast
                    try (var consumerScope = StructuredTaskScope.open()) {
                        var balanceTask = consumerScope.fork(() -> {
                            ValidationResult result = balanceService.validate(request);
                            if (!result.success()) {
                                throw new RuntimeException(result.message());
                            }
                            return result;
                        });

                        var pinTask = consumerScope.fork(() -> {
                            ValidationResult result = pinValidationService.validate(request);
                            if (!result.success()) {
                                throw new RuntimeException(result.message());
                            }
                            return result;
                        });

                        var expirationTask = consumerScope.fork(() -> {
                            ValidationResult result = expirationService.validate(request);
                            if (!result.success()) {
                                throw new RuntimeException(result.message());
                            }
                            return result;
                        });

                        consumerScope.join();

                        return ValidationResult.success("All card validations passed");
                    }
                });

                // Wait for both parallel paths to complete
                globalScope.join();

                merchantTask.get();
                consumerTask.get();
            }

            // Step 3: Transfer amount if all validations passed
            ValidationResult transferResult = balanceService.transfer(
                request.cardNumber(),
                request.merchant(),
                request.amount()
            );
            long processingTime = System.currentTimeMillis() - startTime;

            if (transferResult.success()) {
                String transactionId = UUID.randomUUID().toString();
                System.out.println("✅ FAIL-FAST STRUCTURED transaction completed: " + transactionId +
                        " (in " + processingTime + "ms)");
                return TransactionResult.success(transactionId, request.amount(), processingTime);
            } else {
                System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + transferResult.message() +
                        " (in " + processingTime + "ms)");
                return TransactionResult.failure(transferResult.message(), processingTime);
            }

        } catch (StructuredTaskScope.FailedException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            String failureMessage = e.getCause().getMessage();
            System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + failureMessage +
                             " (in " + processingTime + "ms)");
            System.out.println("   ⚡ Other validations were automatically cancelled!");
            return TransactionResult.failure(failureMessage, processingTime);
        }
    }
}
