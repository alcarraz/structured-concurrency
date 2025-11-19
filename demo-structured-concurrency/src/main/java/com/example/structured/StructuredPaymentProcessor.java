package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/**
 * Structured Concurrency implementation with parallel merchant and consumer validation,
 * followed by nested parallel card validations.
 * <p>
 * Flow:
 * 1. Parallel: Validate Merchant AND Validate Card
 * 2. Parallel (if card OK): Validate Balance, PIN, Expiration
 * 3. Transfer (if all OK)
 */
public class StructuredPaymentProcessor implements StructuredProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    private static final ValidationResult SUCCESS = ValidationResult.success("All validations passed");
    public StructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
    }

    @Override
    public TransactionResult processTransaction(TransactionRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 Starting STRUCTURED transaction processing for merchant " + request.merchant());

        // Step 1: Parallel - Validate Merchant AND Consumer (Card)
        try (var globalScope = StructuredTaskScope.open()) {

            // Fork merchant validation
            var merchantTask = globalScope.fork(() ->
                merchantValidationService.validate(request));

            // Fork consumer validation path (card + nested parallel validations)
            var consumerTask = globalScope.fork(() -> {
                // First validate card
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    return cardResult;
                }

                // Step 2: Parallel - Validate Balance, PIN, and Expiration
                try (var consumerScope = StructuredTaskScope.open()) {
                    var balanceTask = consumerScope.fork(() -> balanceService.validate(request));
                    var pinTask = consumerScope.fork(() -> pinValidationService.validate(request));
                    var expirationTask = consumerScope.fork(() -> expirationService.validate(request));

                    consumerScope.join();

                    // Check all card validation results
                    List<ValidationResult> cardResults = List.of(
                        balanceTask.get(),
                        pinTask.get(),
                        expirationTask.get()
                    );

                    return cardResults.stream()
                        .filter(r -> !r.success())
                        .findFirst()
                        .orElse(SUCCESS);

                }
            });

            // Wait for both parallel paths to complete
            globalScope.join();

            // Check results from both paths
            ValidationResult merchantResult = merchantTask.get();
            ValidationResult consumerResult = consumerTask.get();

            if (!merchantResult.success()) {
                long processingTime = System.currentTimeMillis() - startTime;
                System.out.println("❌ STRUCTURED transaction failed: " + merchantResult.message() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure(merchantResult.message(), processingTime);
            }

            if (!consumerResult.success()) {
                long processingTime = System.currentTimeMillis() - startTime;
                System.out.println("❌ STRUCTURED transaction failed: " + consumerResult.message() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure(consumerResult.message(), processingTime);
            }
        }

        // Step 3: Transfer amount if all validations passed
        ValidationResult transferResult = balanceService.transfer(
            request.cardNumber(),
            request.merchant(),
            request.amount()
        );
        long processingTime = System.currentTimeMillis() - startTime;

        if (transferResult.success()) {
            String transactionId = UUID.randomUUID().toString();
            System.out.println("✅ STRUCTURED transaction completed: " + transactionId +
                    " (in " + processingTime + "ms)");
            return TransactionResult.success(transactionId, request.amount(), processingTime);
        } else {
            System.out.println("❌ STRUCTURED transaction failed: " + transferResult.message() +
                    " (in " + processingTime + "ms)");
            return TransactionResult.failure(transferResult.message(), processingTime);
        }
    }
}
