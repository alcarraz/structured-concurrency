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
        CompletableFuture<ValidationResult> consumerValidation = CompletableFuture
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
                // Create a future that completes as soon as we know the final outcome
                CompletableFuture<ValidationResult> failFast = new CompletableFuture<>();

                // Create validation futures that notify the failFast future
                List<CompletableFuture<ValidationResult>> futures = List.of(
                        validationTask(expirationService, request, card),
                        validationTask(pinValidationService, request, card),
                        validationTask(balanceService, request, card)
                );

                // When all validations complete successfully, complete failFast with success
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        if (!failFast.isDone()) {
                            failFast.complete(ValidationResult.success());
                        }
                    });

                // Return the failFast future - it completes as soon as we know the outcome
                // (first failure OR all successes)
                try {
                    return failFast.join();
                } catch (CompletionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ValidationException ve) {
                        return ValidationResult.failure(ve.getMessage());
                    }
                    throw new RuntimeException("Consumer validation failed");
                }
            });

        // Group top-level validations
        List<CompletableFuture<ValidationResult>> topLevelValidations = List.of(merchantValidation, consumerValidation);

        // Wait for BOTH parallel paths (merchant AND consumer)
        return CompletableFuture.allOf(topLevelValidations.toArray(new CompletableFuture[0]))
            .thenCompose(_ -> {
                // Check all validation results uniformly
                topLevelValidations.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> !r.success())
                    .findFirst()
                    .ifPresent(failure -> {
                        throw new ValidationException(failure);
                    });

                // Step 3: Transfer amount if all validations passed
                logger.debug("✅ All validations passed, proceeding with transfer...");
                return CompletableFuture.runAsync(() -> balanceService.transfer(request));
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

                logger.info("❌ FIXED REACTIVE FAIL-FAST transaction failed: {} (in {}ms)",
                           reason, processingTime);
                logger.debug("   ⚡ Attempted to cancel remaining validations");

                return TransactionResult.failure(reason, processingTime);
            });
    }
    
    private CompletableFuture<ValidationResult> validationTask(CardAwareValidationService service, TransactionRequest request, Card card) {
        return CompletableFuture.supplyAsync(() -> switch(expirationService.validate(request, card)){
            case ValidationResult.Success success -> success;
            case ValidationResult.Failure(String msg) -> throw new ValidationException(msg);
        });
    }

}
