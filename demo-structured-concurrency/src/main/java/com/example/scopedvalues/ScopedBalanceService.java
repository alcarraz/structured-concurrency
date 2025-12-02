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

        DemoUtil.simulateNetworkDelay(500);

        // Simple check: fail if amount > 1000 for demo purposes
        if (request.amount().doubleValue() > 1000) {
            return ValidationResult.failure("Balance Check: Insufficient funds");
        }

        return ValidationResult.success("Balance Check: Validation successful");
    }

    public void transfer() {
        // Access both request and card from scoped values - no parameter passing needed!
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        var card = ScopedPaymentProcessor.CARD.get();

        String cardNumber = request.cardNumber();
        String merchant = request.merchant();

        DemoUtil.simulateNetworkDelay(300);

        logger.info("💸 Transferring {} from card {} to {}", request.amount(),
                   cardNumber.substring(cardNumber.length() - 4), merchant);
    }

}
