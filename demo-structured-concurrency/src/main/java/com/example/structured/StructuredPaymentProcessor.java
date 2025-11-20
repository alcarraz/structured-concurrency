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
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.stream.Stream;

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
    private final MerchantValidationService merchantValidationService;
    private final List<ValidationService> cardValidations;

    private static final ValidationResult SUCCESS = ValidationResult.success("All validations passed");
    public StructuredPaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
        cardValidations = List.of(expirationService, pinValidationService, balanceService);
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
                    List<Subtask<ValidationResult>> cardValidationTasks = cardValidations.stream().map(service -> consumerScope.fork(() -> service.validate(request))).toList();

                    consumerScope.join();

                    // Check all card validation results
                    return cardValidationTasks.stream()
                        .map(Subtask::get)
                        .filter(r -> !r.success())
                        .findFirst()
                        .orElse(SUCCESS);

                }
            });

            // Wait for both parallel paths to complete
            globalScope.join();

            // Check results from both paths and handle failure or transfer
            return Stream.of(merchantTask, consumerTask)
                .map(Subtask::get)
                .filter(r -> !r.success())
                .findFirst()
                .map(result -> {
                    balanceService.releaseAmount(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    System.out.println("❌ STRUCTURED transaction failed: " + result.message() +
                                     " (in " + processingTime + "ms)");
                    return TransactionResult.failure(result.message(), processingTime);
                })
                .orElseGet(() -> {
                    // Step 3: Transfer amount if all validations passed
                    balanceService.transfer(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    String transactionId = UUID.randomUUID().toString();
                    System.out.println("✅ STRUCTURED transaction completed: " + transactionId +
                            " (in " + processingTime + "ms)");
                    return TransactionResult.success(transactionId, request.amount(), processingTime);
                });
        }
    }
}
