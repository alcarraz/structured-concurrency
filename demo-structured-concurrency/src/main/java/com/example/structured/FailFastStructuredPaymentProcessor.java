package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.PinValidationService;

import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/**
 * Fail-fast Structured Concurrency implementation with sequential-parallel flow
 * but using ValidationResult exceptions for fail-fast behavior.
 * <p>
 * This demonstrates structured concurrency's automatic fail-fast and cancellation
 * capabilities while following the new business flow.
 */
public class FailFastStructuredPaymentProcessor implements StructuredProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;

    public FailFastStructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // Step 1: Validate card first (sequential)
        ValidationResult cardResult = cardValidationService.validate(request);
        if (!cardResult.success()) {
            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + cardResult.message() +
                             " (in " + processingTime + "ms)");
            return TransactionResult.failure(cardResult.message(), processingTime);
        }

        // Step 2: Parallel validations with fail-fast using exceptions
        try (var scope = StructuredTaskScope.open()) {
            var balanceTask = scope.fork(() -> {
                ValidationResult result = balanceService.validate(request);
                if (!result.success()) {
                    throw new RuntimeException(result.message());
                }
                return result;
            });

            var pinTask = scope.fork(() -> {
                ValidationResult result = pinValidationService.validate(request);
                if (!result.success()) {
                    throw new RuntimeException(result.message());
                }
                return result;
            });

            var expirationTask = scope.fork(() -> {
                ValidationResult result = expirationService.validate(request);
                if (!result.success()) {
                    throw new RuntimeException(result.message());
                }
                return result;
            });

            // Wait for all parallel validations - fail-fast on first failure
            scope.join();

            // Step 3: Debit amount if all validations passed
            ValidationResult debitResult = balanceService.debit(request.cardNumber(), request.amount());
            long processingTime = System.currentTimeMillis() - startTime;

            if (debitResult.success()) {
                String transactionId = UUID.randomUUID().toString();
                System.out.println("✅ FAIL-FAST STRUCTURED transaction completed: " + transactionId +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.success(transactionId, request.amount(), processingTime);
            } else {
                System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + debitResult.message() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure(debitResult.message(), processingTime);
            }

        } catch (StructuredTaskScope.FailedException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            String failureMessage = e.getCause().getMessage();
            System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + failureMessage +
                             " (in " + processingTime + "ms)");
            System.out.println("   ⚡ Other validations were automatically cancelled!");
            return TransactionResult.failure(failureMessage, processingTime);
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println("💥 FAIL-FAST STRUCTURED transaction error: " + e.getMessage() +
                             " (in " + processingTime + "ms)");
            return TransactionResult.failure("Processing error: " + e.getMessage(), processingTime);
        }
    }
}
