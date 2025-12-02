package com.example.structured;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Structured Concurrency implementation with merchant validation in parallel
 * while consumer validation executes in the main thread.
 * <p>
 * Flow:
 * 1. Fork merchant validation (runs in parallel)
 * 2. Main thread: Validate Card (sequential prerequisite)
 * 3. Nested parallel (if card OK): Validate Balance, PIN, Expiration
 * 4. Wait for merchant completion
 * 5. Transfer (if all OK)
 */
@ApplicationScoped
public class StructuredPaymentProcessor implements StructuredProcessor {
    private static final Logger logger = LogManager.getLogger(StructuredPaymentProcessor.class);

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    @Inject
    public StructuredPaymentProcessor(
            BalanceService balanceService,
            CardValidationService cardValidationService,
            ExpirationService expirationService,
            PinValidationService pinValidationService,
            MerchantValidationService merchantValidationService) {
        this.balanceService = balanceService;
        this.cardValidationService = cardValidationService;
        this.expirationService = expirationService;
        this.pinValidationService = pinValidationService;
        this.merchantValidationService = merchantValidationService;
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        logger.info("🚀 Starting STRUCTURED transaction processing for merchant {}", request.merchant());

        // Step 1: Fork merchant validation, execute consumer in main thread
        try (var globalScope = StructuredTaskScope.open()) {

            // Fork merchant validation (runs in parallel)
            Subtask<ValidationResult> merchantValidation = globalScope.fork(() ->
                merchantValidationService.validate(request));

            // Execute consumer validation directly in main thread
            CardValidationResult cardResult = cardValidationService.validate(request);

            // Pattern match on the result
            if (cardResult instanceof CardValidationResult.Success(Card card)) {
                // Step 2: Parallel - Validate Balance, PIN and Expiration (with Card)
                try (var consumerScope = StructuredTaskScope.open(Joiner.<ValidationResult>allSuccessfulOrThrow())) {
                    // Pass the card to card-aware services
                    consumerScope.fork(() -> expirationService.validate(request, card));
                    consumerScope.fork(() -> pinValidationService.validate(request, card));
                    consumerScope.fork(() -> balanceService.validate(request, card));

                    Optional<CardValidationResult> failure = consumerScope.join()
                            .map(Subtask::get)
                            .filter(ValidationResult.Failure.class::isInstance)
                            .map(ValidationResult.Failure.class::cast)
                            .findFirst()
                            .map(r -> CardValidationResult.failure(r.message()));
                    if (failure.isPresent()) cardResult = failure.get();
                }
            }

            // Wait for merchant validation to complete
            globalScope.join();
            ValidationResult merchantResult = merchantValidation.get();

            if (ValidationResult.success(merchantResult) && cardResult instanceof CardValidationResult.Success(Card card)) {
                balanceService.transfer(request, card);
                long processingTime = System.currentTimeMillis() - startTime;
                String transactionId = UUID.randomUUID().toString();
                logger.info("✅ STRUCTURED transaction completed: {} (in {}ms)",
                        transactionId, processingTime);
                return TransactionResult.success(transactionId, request.amount(), processingTime);
            } else {
                balanceService.releaseAmount(request); //there was a failure, release amount
                String message;
                if (cardResult instanceof CardValidationResult.Failure(String msg)) {
                    message = msg;
                } else if (merchantResult instanceof ValidationResult.Failure(String msg)) {
                    message = msg;
                } else throw new IllegalStateException("Unknown failure");
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("❌ STRUCTURED transaction failed: {} (in {}ms)", message, processingTime);
                return TransactionResult.failure(message, processingTime);
            }
        }
    }
}
