package com.example.services;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class CardValidationService {

    public ValidationResult validate(String cardNumber) {
        DemoUtil.simulateNetworkDelay(300);

        // Simple check: fail if card contains "0000" for demo purposes
        if (cardNumber.contains("0000")) {
            return ValidationResult.failure("Card Validation: Invalid card");
        }
        return ValidationResult.success("Card Validation: Validation successful");
    }

}