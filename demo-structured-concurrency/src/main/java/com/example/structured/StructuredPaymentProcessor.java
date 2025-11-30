package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.services.ValidationService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Structured Concurrency implementation with parallel merchant and consumer validation,
 * followed by nested parallel card validations.
 * <p>
 * Flow:
 * 1. Parallel: Validate Merchant AND Validate Card
 * 2. Parallel (if card OK): Validate Balance, PIN, Expiration
 * 3. Transfer (if all OK)
 */
@ApplicationScoped
public class StructuredPaymentProcessor implements StructuredProcessor {
    private static final Logger logger = LogManager.getLogger(StructuredPaymentProcessor.class);
    private static final ValidationResult SUCCESS = ValidationResult.success("All validations passed");

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;

    @Inject
    public StructuredPaymentProcessor(
            BalanceService balanceService,
            CardValidationService cardValidationService,
            ExpirationService expirationService,
            PinValidationService pinValidationService,
            MerchantValidationService merchantValidationService) {
        this.balanceService = balanceService;
        this.cardValidationService = cardValidationService;
        this.merchantValidationService = merchantValidationService;
        this.cardValidations = List.of(expirationService, pinValidationService, balanceService);
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        logger.info("🚀 Starting STRUCTURED transaction processing for merchant {}", request.merchant());

        // Step 1: Parallel - Validate Merchant AND Consumer (Card)
        try (var globalScope = StructuredTaskScope.open(Joiner.<ValidationResult>allSuccessfulOrThrow())) {

            // Fork merchant validation
            globalScope.fork(() ->
                merchantValidationService.validate(request));

            // Fork consumer validation path (card + nested parallel validations)
            globalScope.fork(() -> {
                // First validate card
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    return cardResult;
                }

                // Step 2: Parallel - Validate Balance, PIN and Expiration
                try (var consumerScope = StructuredTaskScope.open(Joiner.<ValidationResult>allSuccessfulOrThrow())) {
                    cardValidations.forEach(
                            service -> consumerScope.fork(
                                    () -> service.validate(request)
                            )
                    );

                    return consumerScope.join()
                            .map(Subtask::get)
                            .filter(r -> !r.success())
                            .findFirst()
                            .orElse(SUCCESS);
                    
                }
            });

            // Wait for both parallel paths to complete
            return globalScope.join()
                .map(Subtask::get)
                .filter(r -> !r.success())
                .findFirst()
                .map(result -> {
                    balanceService.releaseAmount(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    logger.info("❌ STRUCTURED transaction failed: {} (in {}ms)",
                               result.message(), processingTime);
                    return TransactionResult.failure(result.message(), processingTime);
                })
                .orElseGet(() -> {
                    // Step 3: Transfer amount if all validations passed
                    balanceService.transfer(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    String transactionId = UUID.randomUUID().toString();
                    logger.info("✅ STRUCTURED transaction completed: {} (in {}ms)",
                               transactionId, processingTime);
                    return TransactionResult.success(transactionId, request.amount(), processingTime);
                });
        }
    }
}
