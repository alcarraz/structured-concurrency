package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardValidationService implements ValidationService {

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(300);

        String cardNumber = request.cardNumber();

        // Simple check: fail if card contains "0000" for demo purposes
        if (cardNumber.contains("0000")) {
            return ValidationResult.failure("Card Validation: Invalid card");
        }
        return ValidationResult.success("Card Validation: Validation successful");
    }

}
