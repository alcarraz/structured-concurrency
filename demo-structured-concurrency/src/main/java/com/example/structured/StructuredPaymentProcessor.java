package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.PinValidationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/**
 * Structured Concurrency implementation with sequential-parallel flow
 * (same logic as ReactivePaymentProcessor) for fair comparison.
 * <p>
 * This demonstrates that structured concurrency provides cleaner code
 * structure while maintaining the same business logic flow.
 */
public class StructuredPaymentProcessor implements StructuredProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;

    public StructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 Starting STRUCTURED transaction processing for customer " + request.customerId());

        // Step 1: Validate card first (sequential)
        ValidationResult cardResult = cardValidationService.validate(request.cardNumber());
        if (!cardResult.success()) {
            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println("❌ STRUCTURED transaction failed: " + cardResult.message() +
                             " (in " + processingTime + "ms)");
            return TransactionResult.failure(cardResult.message(), processingTime);
        }

        // Step 2: Parallel validations if card is valid
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.allSuccessfulOrThrow())) {
            var balanceTask = scope.fork(() ->
                balanceService.validate(request.cardNumber(), request.amount()));
            var pinTask = scope.fork(() ->
                pinValidationService.validate(request.cardNumber(), request.pin()));
            var expirationTask = scope.fork(() ->
                expirationService.validate(request.cardNumber(), request.expirationDate()));

            // Wait for all parallel validations to complete
            scope.join();

            // Check all parallel validation results
            List<ValidationResult> results = List.of(
                balanceTask.get(),
                pinTask.get(),
                expirationTask.get()
            );

            Optional<ValidationResult> failure = results.stream()
                .filter(r -> !r.success())
                .findFirst();

            if (failure.isPresent()) {
                long processingTime = System.currentTimeMillis() - startTime;
                System.out.println("❌ STRUCTURED transaction failed: " + failure.get().message() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure(failure.get().message(), processingTime);
            }


        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println("💥 STRUCTURED transaction error: " + e.getMessage() +
                             " (in " + processingTime + "ms)");
            return TransactionResult.failure("Processing error: " + e.getMessage(), processingTime);
        }
        // Step 3: Debit amount if all validations passed
        ValidationResult debitResult = balanceService.debit(request.cardNumber(), request.amount());
        long processingTime = System.currentTimeMillis() - startTime;

        if (debitResult.success()) {
            String transactionId = UUID.randomUUID().toString();
            System.out.println("✅ STRUCTURED transaction completed: " + transactionId +
                    " (in " + processingTime + "ms)");
            return TransactionResult.success(transactionId, request.amount(), processingTime);
        } else {
            System.out.println("❌ STRUCTURED transaction failed: " + debitResult.message() +
                    " (in " + processingTime + "ms)");
            return TransactionResult.failure(debitResult.message(), processingTime);
        }

    }
}
