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

/**
 * Fail-fast Structured Concurrency implementation with parallel merchant and consumer validation,
 * followed by nested parallel card validations.
 * <p>
 * Flow:
 * 1. Parallel: Validate Merchant AND Validate Card (fail-fast)
 * 2. Parallel (if card OK): Validate Balance, PIN, Expiration (fail-fast)
 * 3. Transfer (if all OK)
 * <p>
 * This demonstrates structured concurrency's automatic fail-fast and cancellation
 * capabilities - when any validation fails, remaining tasks are automatically cancelled.
 */
public class FailFastStructuredPaymentProcessor implements StructuredProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;

    public FailFastStructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
        this.cardValidations = List.of(expirationService, pinValidationService, balanceService);
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 Starting FAIL-FAST STRUCTURED transaction processing for merchant " + request.merchant());

        try {
            // Step 1: Parallel - Validate Merchant AND Consumer (Card) with fail-fast
            try (var globalScope = StructuredTaskScope.open()) {

                // Fork merchant validation
                createValidationTask(merchantValidationService, request, globalScope);

                // Fork consumer validation path (card + nested parallel validations)
                globalScope.fork(() -> {
                    // First validate card
                    createValidationTask(cardValidationService, request, globalScope);

                    // Step 2: Parallel - Validate Balance, PIN, and Expiration with fail-fast
                    try (var consumerScope = StructuredTaskScope.open()) {
                        cardValidations.forEach(service -> createValidationTask(service, request, consumerScope));

                        consumerScope.join();

                        return ValidationResult.success("All card validations passed");
                    }
                });

                // Wait for both parallel paths to complete
                globalScope.join();

            }

            // Step 3: Transfer amount if all validations passed
            balanceService.transfer(request);
            long processingTime = System.currentTimeMillis() - startTime;

            String transactionId = UUID.randomUUID().toString();
            System.out.println("✅ FAIL-FAST STRUCTURED transaction completed: " + transactionId +
                    " (in " + processingTime + "ms)");
            return TransactionResult.success(transactionId, request.amount(), processingTime);

        } catch (StructuredTaskScope.FailedException e) {
            balanceService.releaseAmount(request);
            long processingTime = System.currentTimeMillis() - startTime;
            String failureMessage = e.getCause().getMessage();
            System.out.println("❌ FAIL-FAST STRUCTURED transaction failed: " + failureMessage +
                             " (in " + processingTime + "ms)");
            System.out.println("   ⚡ Other validations were automatically cancelled!");
            return TransactionResult.failure(failureMessage, processingTime);
        }
    }

    private void createValidationTask(ValidationService service, TransactionRequest request, StructuredTaskScope<Object, Void> scope) {
        scope.fork(() -> {
            ValidationResult result = service.validate(request);
            if (!result.success()) {
                throw new RuntimeException(result.message());
            }
        });
    }
}
