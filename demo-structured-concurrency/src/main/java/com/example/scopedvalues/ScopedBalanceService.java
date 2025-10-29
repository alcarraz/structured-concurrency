package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import java.math.BigDecimal;

public class ScopedBalanceService {

    public ValidationResult validate(String cardNumber, BigDecimal amount) {
        // Access the scoped value directly - no parameter passing needed!
        RequestContext context = ScopedPaymentProcessor.REQUEST_CONTEXT.get();
        auditLog("Starting balance validation for card: " + cardNumber.substring(cardNumber.length() - 4));

        DemoUtil.simulateNetworkDelay(500);

        // Simple check: fail if amount > 1000 for demo purposes
        if (amount.doubleValue() > 1000) {
            auditLog("Balance validation failed: Insufficient funds");
            return ValidationResult.failure("Balance Check: Insufficient funds");
        }

        auditLog("Balance validation successful");
        return ValidationResult.success("Balance Check: Validation successful");
    }

    public ValidationResult debit(String cardNumber, BigDecimal amount) {
        RequestContext context = ScopedPaymentProcessor.REQUEST_CONTEXT.get();
        auditLog("Starting debit for card: " + cardNumber.substring(cardNumber.length() - 4) + ", amount: " + amount);

        DemoUtil.simulateNetworkDelay(300);

        auditLog("Debit successful");
        System.out.println("💳 Debiting " + amount + " from card ***" + cardNumber.substring(cardNumber.length() - 4));
        return ValidationResult.success("Balance Debit: Amount successfully debited");
    }

    private void auditLog(String message) {
        RequestContext context = ScopedPaymentProcessor.REQUEST_CONTEXT.get();
        System.out.println("💰 BALANCE [" + context.correlationId() + "] " + message);
    }

}
