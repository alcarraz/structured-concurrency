package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

public class ScopedPaymentProcessor {
    private static final Logger logger = LogManager.getLogger(ScopedPaymentProcessor.class);

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


        // Run the entire transaction within the scoped value context
        return ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
            try {
                // Step 1: Validate card first (sequential)
                ValidationResult cardResult = cardValidationService.validate();
                if (!cardResult.success()) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                                     cardResult.message(), processingTime);
                    return TransactionResult.failure(cardResult.message(), processingTime);
                }

                logger.info("✅ Card validation passed, proceeding with parallel validations...");

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
                        logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                                         failureReason, processingTime);
                        return TransactionResult.failure(failureReason, processingTime);
                    }

                    logger.info("✅ All validations passed, proceeding with debit...");

                    // Step 3: Debit the amount
                    ValidationResult debitResult = balanceService.debit();
                    if (!debitResult.success()) {
                        long processingTime = System.currentTimeMillis() - startTime;
                        logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                                         debitResult.message(), processingTime);
                        return TransactionResult.failure(debitResult.message(), processingTime);
                    }

                    // Success!
                    long processingTime = System.currentTimeMillis() - startTime;
                    String transactionId = UUID.randomUUID().toString();
                    logger.info("✅ SCOPED VALUES transaction completed: {} (in {}ms)",
                                     transactionId, processingTime);
                    return TransactionResult.success(transactionId, request.amount(), processingTime);
                }

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("💥 SCOPED VALUES transaction error: {} (in {}ms)",
                                 e.getMessage(), processingTime);
                return TransactionResult.failure("Processing error: " + e.getMessage(), processingTime);
            }
        });
    }
}
