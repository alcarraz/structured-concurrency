package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScopedBalanceService {
    private static final Logger logger = LogManager.getLogger(ScopedBalanceService.class);

    public ValidationResult validate() {
        // Access the scoped value directly - no parameter passing needed!
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        DemoUtil.simulateNetworkDelay(500);

        // Simple check: fail if amount > 1000 for demo purposes
        if (request.amount().doubleValue() > 1000) {
            return ValidationResult.failure("Balance Check: Insufficient funds");
        }

        return ValidationResult.success("Balance Check: Validation successful");
    }

    public ValidationResult debit() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        DemoUtil.simulateNetworkDelay(300);

        logger.info("💳 Debiting " + request.amount() + " from card ***" + cardNumber.substring(cardNumber.length() - 4));
        return ValidationResult.success("Balance Debit: Amount successfully debited");
    }

}
