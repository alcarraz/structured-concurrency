package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BasicReactivePaymentProcessor implements ReactivePaymentProcessor {
    private final BalanceService balanceService;
    private final CardValidationService cardValidationService;
    private final ExpirationService expirationService;
    private final PinValidationService pinValidationService;
    private final MerchantValidationService merchantValidationService;

    public BasicReactivePaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
        this.merchantValidationService = new MerchantValidationService();
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Run merchant validation in parallel with ENTIRE consumer validation flow

        // PATH A: Merchant validation (runs independently)
        CompletableFuture<ValidationResult> merchantValidation =
            CompletableFuture.supplyAsync(() -> merchantValidationService.validate(request));

        // PATH B: Consumer validation (card → nested parallel validations)
        CompletableFuture<ValidationResult> consumerValidation =
            CompletableFuture.supplyAsync(() -> {
                // B1: Validate card first (sequential within consumer path)
                ValidationResult cardResult = cardValidationService.validate(request);
                if (!cardResult.success()) {
                    return cardResult;
                }

                // B2: Nested parallel validations (balance, PIN, expiration)
                CompletableFuture<ValidationResult> balanceValidation =
                    CompletableFuture.supplyAsync(() -> balanceService.validate(request));
                CompletableFuture<ValidationResult> pinValidation =
                    CompletableFuture.supplyAsync(() -> pinValidationService.validate(request));
                CompletableFuture<ValidationResult> expirationValidation =
                    CompletableFuture.supplyAsync(() -> expirationService.validate(request));

                // Wait for all nested validations
                CompletableFuture.allOf(balanceValidation, pinValidation, expirationValidation).join();

                // Check nested validation results
                List<ValidationResult> results = List.of(
                    balanceValidation.join(),
                    pinValidation.join(),
                    expirationValidation.join()
                );

                Optional<ValidationResult> failure = results.stream()
                    .filter(r -> !r.success())
                    .findFirst();

                return failure.orElse(ValidationResult.success("All card validations passed"));
            });

        // Wait for BOTH parallel paths (merchant + complete consumer flow)
        return CompletableFuture.allOf(merchantValidation, consumerValidation)
            .thenCompose(_ -> {
                ValidationResult merchantResult = merchantValidation.join();
                ValidationResult consumerResult = consumerValidation.join();

                // Check merchant result
                if (!merchantResult.success()) {
                    balanceService.releaseAmount(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    System.out.println("❌ REACTIVE transaction failed: " + merchantResult.message() +
                                     " (in " + processingTime + "ms)");
                    return CompletableFuture.completedFuture(
                        TransactionResult.failure(merchantResult.message(), processingTime));
                }

                // Check consumer result
                if (!consumerResult.success()) {
                    balanceService.releaseAmount(request);
                    long processingTime = System.currentTimeMillis() - startTime;
                    System.out.println("❌ REACTIVE transaction failed: " + consumerResult.message() +
                                     " (in " + processingTime + "ms)");
                    return CompletableFuture.completedFuture(
                        TransactionResult.failure(consumerResult.message(), processingTime));
                }

                // Step 2: Transfer amount if both paths succeeded
                return CompletableFuture
                    .runAsync(() -> balanceService.transfer(request))
                    .thenApply(_ -> {
                        long processingTime = System.currentTimeMillis() - startTime;

                        String transactionId = UUID.randomUUID().toString();
                        System.out.println("✅ REACTIVE transaction completed: " + transactionId +
                                         " (in " + processingTime + "ms)");
                        return TransactionResult.success(transactionId, request.amount(), processingTime);
                    });
            })
            .exceptionally(throwable -> {
                long processingTime = System.currentTimeMillis() - startTime;
                System.out.println("💥 REACTIVE transaction error: " + throwable.getMessage() +
                                 " (in " + processingTime + "ms)");
                return TransactionResult.failure("Processing error: " + throwable.getMessage(), processingTime);
            });
    }

}
