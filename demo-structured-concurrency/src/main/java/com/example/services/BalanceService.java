package com.example.services;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import java.math.BigDecimal;

public class BalanceService {

    public ValidationResult validate(String cardNumber, BigDecimal amount) {
        DemoUtil.simulateNetworkDelay(500);

        // Simple check: fail if amount > 1000 for demo purposes
        if (amount.doubleValue() > 1000) {
            return ValidationResult.failure("Balance Check: Insufficient funds");
        }
        return ValidationResult.success("Balance Check: Validation successful");
    }

    public ValidationResult debit(String cardNumber, BigDecimal amount) {
        DemoUtil.simulateNetworkDelay(500);

        // Simulate debit operation
        System.out.println("💳 Debiting " + amount + " from card " + cardNumber.substring(cardNumber.length() - 4));
        return ValidationResult.success("Balance Debit: Amount successfully debited");
    }

    public ValidationResult transfer(String cardNumber, String merchant, BigDecimal amount) {
        DemoUtil.simulateNetworkDelay(500);

        // Simulate transfer operation
        System.out.println("💸 Transferring " + amount + " from card " + cardNumber.substring(cardNumber.length() - 4) + " to " + merchant);
        return ValidationResult.success("Transfer: Amount successfully transferred");
    }

}
