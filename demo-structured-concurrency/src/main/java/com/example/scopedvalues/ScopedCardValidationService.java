package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class ScopedCardValidationService {

    public ValidationResult validate() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        DemoUtil.simulateNetworkDelay(300);

        // Simple check: fail if card contains "0000" for demo purposes
        if (cardNumber.contains("0000")) {
            return ValidationResult.failure("Card Validation: Invalid card");
        }

        return ValidationResult.success("Card Validation: Validation successful");
    }

}
