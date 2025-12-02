package com.example.reactive;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardAwareValidationService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.services.ValidationException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "Fixed" reactive implementation that attempts to fail fast using CompletableFuture
 * and exception handling. This shows how one COULD try to fix reactive programming
 * to achieve fail-fast behavior, but demonstrates the complexity required.
 * <p>
 * Note: Even this "fix" is significantly more complex than structured concurrency's
 * built-in fail-fast behavior, requiring manual cancellation logic and
 * careful exception handling that is prone to race conditions.
 */
@ApplicationScoped
public class FixedReactiveFailFastPaymentProcessor implements ReactivePaymentProcessor {
    private static final Logger logger = LogManager.getLogger(FixedReactiveFailFastPaymentProcessor.class);

    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    @Inject
    public FixedReactiveFailFastPaymentProcessor(
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
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // PATH A: Merchant validation (runs independently with fail-fast)
        CompletableFuture<ValidationResult> merchantValidation = CompletableFuture
            .supplyAsync(() -> switch (merchantValidationService.validate(request)) {
                case ValidationResult.Success s -> s;
                case ValidationResult.Failure(String m) -> throw new ValidationException(m);
            });

        // PATH B: Consumer validation (card → nested parallel validations with fail-fast)
        CompletableFuture<Card> consumerValidation = CompletableFuture
            .supplyAsync(() -> {
                // B1: Validate card first
                CardValidationResult cardResult = cardValidationService.validate(request);

                // Pattern match - throw exception on failure
                Card card = switch (cardResult) {
                    case CardValidationResult.Success(Card c) -> c;
                    case CardValidationResult.Failure(String msg) ->
                        throw new ValidationException(msg);
                };

                // B2: Nested parallel validations with TRUE fail-fast coordination (with Card)
                CompletableFuture<Card> failFast = new CompletableFuture<>();

                // Create validation futures with exception handlers
                List<CompletableFuture<ValidationResult>> futures = List.of(
                        validationTask(expirationService, request, card),
                        validationTask(pinValidationService, request, card),
                        validationTask(balanceService, request, card)
                );

                // Set up handlers for each future to complete failFast on first failure
                for (CompletableFuture<ValidationResult> future : futures) {
                    future.exceptionally(throwable -> {
                        failFast.completeExceptionally(throwable);
                        return null;  // Return value doesn't matter, failFast is already completed
                    });
                }

                // When all validations complete successfully, complete failFast with Card
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        if (!failFast.isDone()) {
                            failFast.complete(card);
                        }
                    });

                // Return the failFast future - completes on first failure OR all successes
                try {
                    return failFast.join();
                } catch (CompletionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ValidationException ve) {
                        throw ve;
                    }
                    throw new RuntimeException("Consumer validation failed");
                }
            });

        // Wait for BOTH parallel paths (merchant AND consumer)
        return CompletableFuture.allOf(merchantValidation, consumerValidation)
            .thenCompose(_ -> {
                // If we get here, all validations passed (exceptions would have been thrown otherwise)
                // Extract the card for transfer
                Card card = consumerValidation.join();

                // Step 3: Transfer amount if all validations passed
                logger.debug("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request, card));
            })
            .thenApply(_ -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String transactionId = UUID.randomUUID().toString();

                logger.info("✅ FIXED REACTIVE FAIL-FAST transaction completed: {} (in {}ms)",
                           transactionId, processingTime);

                return TransactionResult.success(transactionId, request.amount(), processingTime);
            })
            .exceptionally(throwable -> {
                long processingTime = System.currentTimeMillis() - startTime;
                String reason = throwable.getMessage();

                // Release any locked balance
                balanceService.releaseAmount(request);

                logger.info("❌ FIXED REACTIVE FAIL-FAST transaction failed: {} (in {}ms)",
                           reason, processingTime);
                logger.debug("   ⚡ Attempted to cancel remaining validations");

                return TransactionResult.failure(reason, processingTime);
            });
    }
    
    private CompletableFuture<ValidationResult> validationTask(CardAwareValidationService service, TransactionRequest request, Card card) {
        return CompletableFuture.supplyAsync(() -> switch(service.validate(request, card)){
            case ValidationResult.Success success -> success;
            case ValidationResult.Failure(String msg) -> throw new ValidationException(msg);
        });
    }

}
