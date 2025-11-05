package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class ScopedBalanceService {

    public ValidationResult validate() {
        // Access the scoped value directly - no parameter passing needed!
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        auditLog("Starting balance validation for card: " + cardNumber.substring(cardNumber.length() - 4));

        DemoUtil.simulateNetworkDelay(500);

        // Simple check: fail if amount > 1000 for demo purposes
        if (request.amount().doubleValue() > 1000) {
            auditLog("Balance validation failed: Insufficient funds");
            return ValidationResult.failure("Balance Check: Insufficient funds");
        }

        auditLog("Balance validation successful");
        return ValidationResult.success("Balance Check: Validation successful");
    }

    public ValidationResult debit() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        auditLog("Starting debit for card: " + cardNumber.substring(cardNumber.length() - 4) + ", amount: " + request.amount());

        DemoUtil.simulateNetworkDelay(300);

        auditLog("Debit successful");
        System.out.println("💳 Debiting " + request.amount() + " from card ***" + cardNumber.substring(cardNumber.length() - 4));
        return ValidationResult.success("Balance Debit: Amount successfully debited");
    }

    private void auditLog(String message) {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        System.out.println("💰 BALANCE [Customer: " + request.customerId() + "] " + message);
    }

}
