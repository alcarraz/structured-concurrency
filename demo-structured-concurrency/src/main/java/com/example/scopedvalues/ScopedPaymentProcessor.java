package com.example.scopedvalues;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final ScopedMerchantValidationService merchantValidationService;

    public ScopedPaymentProcessor(
            ScopedCardValidationService cardValidationService, 
            ScopedBalanceService balanceService, 
            ScopedExpirationService expirationService, 
            ScopedPinValidationService pinValidationService, 
            ScopedMerchantValidationService merchantValidationService
    ) {
        this.balanceService = balanceService;
        this.cardValidationService = cardValidationService;
        this.expirationService = expirationService;
        this.pinValidationService = pinValidationService;
        this.merchantValidationService = merchantValidationService;
    }

    public TransactionResult processTransaction(TransactionRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.info("🚀 Starting SCOPED VALUES transaction processing for merchant {}", request.merchant());

        // Establish TRANSACTION_REQUEST scoped context for entire operation
        return ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
            try {
                // Level 1: Global scope with parallel merchant + consumer paths
                try (var globalScope = StructuredTaskScope.open()) {

                    // PATH A: Fork merchant validation
                    createValidationTask(merchantValidationService, globalScope);
                    
                    // PATH B: Fork consumer validation (card + nested validations)
                    // Returns CardValidationResult so we can extract the Card after join
                    StructuredTaskScope.Subtask<Card> consumerValidation =
                        globalScope.fork(() -> {
                            // Sequential: Validate card first
                            CardValidationResult cardResult = cardValidationService.validate();

                            // Pattern match on card result
                            Card card = switch (cardResult) {
                                case CardValidationResult.Success(Card c) -> c;
                                case CardValidationResult.Failure(String msg) ->
                                    throw new com.example.services.ValidationException(msg);
                            };

                            // Establish CARD scoped context for nested validations
                            return ScopedValue.where(CARD, card).call(() -> {
                                // Level 2: Nested scope with parallel balance/expiration/pin
                                try (var consumerScope = StructuredTaskScope.open()) {

                                    // Fork parallel validations (inherit both scoped values)
                                    createValidationTask(balanceService, consumerScope);
                                    createValidationTask(expirationService, consumerScope);
                                    createValidationTask(pinValidationService, consumerScope);

                                    // Wait for all nested validations
                                    consumerScope.join();

                                    // Return the CardValidationResult with the card
                                    return card;
                                }
                            });
                        });

                    // Wait for both parallel paths (merchant + consumer)
                    globalScope.join();

                    Card card = consumerValidation.get();

                    // All validations passed - perform transfer
                    // Re-establish CARD context for transfer
                    return ScopedValue.where(CARD, card).call(() -> {
                        balanceService.transfer();

                        long processingTime = System.currentTimeMillis() - startTime;
                        String transactionId = java.util.UUID.randomUUID().toString();
                        logger.info("✅ SCOPED VALUES transaction completed: {} (in {}ms)",
                                   transactionId, processingTime);
                        return TransactionResult.success(transactionId, request.amount(), processingTime);
                    });
                }

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                String failureMessage = e.getMessage();
                logger.info("❌ SCOPED VALUES transaction failed: {} (in {}ms)",
                           failureMessage, processingTime);
                return TransactionResult.failure(failureMessage, processingTime);
            }
        });
    }

    /**
     * Creates a validation task that throws ValidationException on failure.
     * Pattern matches on ValidationResult and propagates failures as exceptions
     * for fail-fast behavior in structured concurrency.
     */
    private void createValidationTask(
        ScopedValidationService service,
        StructuredTaskScope<Object, Void> scope
    ) {
        scope.fork(() -> {
            if (service.validate() instanceof ValidationResult.Failure(String msg)) {
                throw new com.example.services.ValidationException(msg);
            }
        });
    }
}
