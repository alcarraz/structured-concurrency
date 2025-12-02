package com.example.scopedvalues;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.MerchantValidationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

public class ScopedPaymentProcessor {
    private static final Logger logger = LogManager.getLogger(ScopedPaymentProcessor.class);

    // Define scoped values for the transaction request and card
    public static final ScopedValue<TransactionRequest> TRANSACTION_REQUEST = ScopedValue.newInstance();
    public static final ScopedValue<Card> CARD = ScopedValue.newInstance();

    private final ScopedBalanceService balanceService;
    private final ScopedCardValidationService cardValidationService;
    private final ScopedExpirationService expirationService;
    private final ScopedPinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    public ScopedPaymentProcessor(com.example.repository.CardRepository cardRepository, MerchantValidationService merchantValidationService) {
        this.balanceService = new ScopedBalanceService();
        this.cardValidationService = new ScopedCardValidationService(cardRepository);
        this.expirationService = new ScopedExpirationService();
        this.pinValidationService = new ScopedPinValidationService();
        this.merchantValidationService = merchantValidationService;
    }

    public TransactionResult processTransaction(TransactionRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        return ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
            try (var globalScope = StructuredTaskScope.open()) {
                // Fork merchant validation
                StructuredTaskScope.Subtask<ValidationResult> merchantValidation = globalScope.fork(() ->
                        merchantValidationService.validate(request));
                
            }
        });
        // Run the entire transaction within the scoped value context
        return ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
            try {
                // Step 1: Validate card first (sequential)
                CardValidationResult cardResult = cardValidationService.validate();

                // Pattern match on the result
                return switch (cardResult) {
                    case CardValidationResult.Success(Card card) -> {
                        logger.info("✅ Card validation passed, proceeding with parallel validations...");

                        // Set the card in scoped context for nested validations
                        yield ScopedValue.where(CARD, card).call(() -> {
                            // Step 2: Parallel validations with scoped context (Card available via ScopedValue)
                            try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.allSuccessfulOrThrow())) {
                                // Fork parallel validation tasks - they automatically inherit the scoped context
                                var balanceTask = scope.fork(balanceService::validate);
                                var expirationTask = scope.fork(expirationService::validate);
                                var pinTask = scope.fork(pinValidationService::validate);

                                // Wait for all parallel tasks to complete
                                scope.join().;

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
                                        .filter(ValidationResult.Failure.class::isInstance)
                                        .map(ValidationResult.Failure.class::cast)
                                        .map(ValidationResult.Failure::message)
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("Unknown validation error"));

                                    long processingTime = System.currentTimeMillis() - startTime;
                                    logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                                                     failureReason, processingTime);
                                    return TransactionResult.failure(failureReason, processingTime);
                                }

                                logger.info("✅ All validations passed, proceeding with transfer...");

                                // Step 3: Transfer the amount (uses scoped values internally)
                                balanceService.transfer();

                                // Success!
                                long processingTime = System.currentTimeMillis() - startTime;
                                String transactionId = UUID.randomUUID().toString();
                                logger.info("✅ SCOPED VALUES transaction completed: {} (in {}ms)",
                                                 transactionId, processingTime);
                                return TransactionResult.success(transactionId, request.amount(), processingTime);
                            }
                        });
                    }
                    case CardValidationResult.Failure(String message) -> {
                        long processingTime = System.currentTimeMillis() - startTime;
                        logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                                         message, processingTime);
                        yield TransactionResult.failure(message, processingTime);
                    }
                };

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("💥 SCOPED VALUES transaction error: {} (in {}ms)",
                                 e.getMessage(), processingTime);
                return TransactionResult.failure("Processing error: " + e.getMessage(), processingTime);
            }
        });
    }
}
