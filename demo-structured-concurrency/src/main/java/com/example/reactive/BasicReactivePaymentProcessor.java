package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.model.ValidationResult;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
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

    public BasicReactivePaymentProcessor() {
        this.balanceService = new BalanceService();
        this.cardValidationService = new CardValidationService();
        this.expirationService = new ExpirationService();
        this.pinValidationService = new PinValidationService();
    }

    @Override
    public CompletableFuture<TransactionResult> processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        System.out.println("🔄 Starting REACTIVE transaction processing for customer " + request.customerId());

        // Step 1: Validate card first (sequential)
        return CompletableFuture
            .supplyAsync(() -> cardValidationService.validate(request.cardNumber()))
            .thenCompose(cardResult -> {
                if (!cardResult.success()) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    System.out.println("❌ REACTIVE transaction failed: " + cardResult.message() +
                                     " (in " + processingTime + "ms)");
                    return CompletableFuture.completedFuture(
                        TransactionResult.failure(cardResult.message(), processingTime));
                }

                // Step 2: Parallel validations if card is valid
                CompletableFuture<ValidationResult> balanceValidation =
                    CompletableFuture.supplyAsync(() ->
                        balanceService.validate(request.cardNumber(), request.amount()));
                CompletableFuture<ValidationResult> pinValidation =
                    CompletableFuture.supplyAsync(() ->
                        pinValidationService.validate(request.cardNumber(), request.pin()));
                CompletableFuture<ValidationResult> expirationValidation =
                    CompletableFuture.supplyAsync(() ->
                        expirationService.validate(request.cardNumber(), request.expirationDate()));

                return CompletableFuture.allOf(balanceValidation, pinValidation, expirationValidation)
                    .thenCompose(_ -> {
                        // Check all parallel validation results
                        List<ValidationResult> results = List.of(
                            balanceValidation.join(),
                            pinValidation.join(),
                            expirationValidation.join()
                        );

                        Optional<ValidationResult> failure = results.stream()
                            .filter(r -> !r.success())
                            .findFirst();

                        if (failure.isPresent()) {
                            long processingTime = System.currentTimeMillis() - startTime;
                            System.out.println("❌ REACTIVE transaction failed: " + failure.get().message() +
                                             " (in " + processingTime + "ms)");
                            return CompletableFuture.completedFuture(
                                TransactionResult.failure(failure.get().message(), processingTime));
                        }

                        // Step 3: Debit amount if all validations passed
                        return CompletableFuture
                            .supplyAsync(() -> balanceService.debit(request.cardNumber(), request.amount()))
                            .thenApply(debitResult -> {
                                long processingTime = System.currentTimeMillis() - startTime;

                                if (debitResult.success()) {
                                    String transactionId = UUID.randomUUID().toString();
                                    System.out.println("✅ REACTIVE transaction completed: " + transactionId +
                                                     " (in " + processingTime + "ms)");
                                    return TransactionResult.success(transactionId, request.amount(), processingTime);
                                } else {
                                    System.out.println("❌ REACTIVE transaction failed: " + debitResult.message() +
                                                     " (in " + processingTime + "ms)");
                                    return TransactionResult.failure(debitResult.message(), processingTime);
                                }
                            });
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
