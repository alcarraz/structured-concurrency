package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

public class ScopedPaymentProcessor {

    // Define scoped value for the transaction request
    public static final ScopedValue<TransactionRequest> TRANSACTION_REQUEST = ScopedValue.newInstance();

    private final ScopedBalanceService balanceService;
    private final ScopedCardValidationService cardValidationService;
    private final ScopedExpirationService expirationService;
    private final ScopedPinValidationService pinValidationService;

    public ScopedPaymentProcessor() {
        this.balanceService = new ScopedBalanceService();
        this.cardValidationService = new ScopedCardValidationService();
        this.expirationService = new ScopedExpirationService();
        this.pinValidationService = new ScopedPinValidationService();
    }

    public TransactionResult processTransaction(TransactionRequest request) throws Exception {
        long startTime = System.currentTimeMillis();

        System.out.println("🔗 Starting SCOPED VALUES transaction processing for customer " + request.customerId());

        // Run the entire transaction within the scoped value context
        return ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
            try {
                // Step 1: Validate card first (sequential)
                ValidationResult cardResult = cardValidationService.validate();
                if (!cardResult.success()) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    auditLog("Transaction failed: " + cardResult.message());
                    System.out.println("❌ SCOPED VALUES transaction failed: " + cardResult.message() +
                                     " (in " + processingTime + "ms)");
                    return TransactionResult.failure(cardResult.message(), processingTime);
                }

                System.out.println("✅ Card validation passed, proceeding with parallel validations...");

                // Step 2: Parallel validations with scoped context
                try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAll())) {
                    // Fork parallel validation tasks - they automatically inherit the scoped context
                    var balanceTask = scope.fork(balanceService::validate);
                    var expirationTask = scope.fork(expirationService::validate);
                    var pinTask = scope.fork(pinValidationService::validate);

                    // Wait for all parallel tasks to complete
                    scope.join();

                    // Collect parallel validation results
                    List<ValidationResult> parallelResults = List.of(
                        balanceTask.get(),
                        expirationTask.get(),
                        pinTask.get()
                    );

                    // Check if all parallel validations passed
                    boolean allValid = parallelResults.stream().allMatch(ValidationResult::success);
                    if (!allValid) {
                        String failureReason = parallelResults.stream()
                            .filter(r -> !r.success())
                            .map(ValidationResult::message)
                            .findFirst()
                            .orElse("Unknown validation error");

                        long processingTime = System.currentTimeMillis() - startTime;
                        auditLog("Transaction failed: " + failureReason);
                        System.out.println("❌ SCOPED VALUES transaction failed: " + failureReason +
                                         " (in " + processingTime + "ms)");
                        return TransactionResult.failure(failureReason, processingTime);
                    }

                    System.out.println("✅ All validations passed, proceeding with debit...");

                    // Step 3: Debit the amount
                    ValidationResult debitResult = balanceService.debit();
                    if (!debitResult.success()) {
                        long processingTime = System.currentTimeMillis() - startTime;
                        auditLog("Transaction failed: " + debitResult.message());
                        System.out.println("❌ SCOPED VALUES transaction failed: " + debitResult.message() +
                                         " (in " + processingTime + "ms)");
                        return TransactionResult.failure(debitResult.message(), processingTime);
                    }

                    // Success!
                    long processingTime = System.currentTimeMillis() - startTime;
                    String transactionId = UUID.randomUUID().toString();
                    auditLog("Transaction completed successfully: " + transactionId);
                    System.out.println("✅ SCOPED VALUES transaction completed: " + transactionId +
                                     " (in " + processingTime + "ms)");
                    return TransactionResult.success(transactionId, request.amount(), processingTime);
                }

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                auditLog("Transaction error: " + e.getMessage());
                System.out.println("💥 SCOPED VALUES transaction error: " + e.getMessage() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure("Processing error: " + e.getMessage(), processingTime);
            }
        });
    }

    private void auditLog(String message) {
        TransactionRequest req = TRANSACTION_REQUEST.get();
        System.out.println("📝 AUDIT [Customer: " + req.customerId() + "] " + message);
    }
}
