package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

import java.util.Map;

public class ScopedPinValidationService {
    private static final Map<String, String> cardPins = Map.of(
        "4532-1234-5678-9012", "1234",
        "5555-4444-3333-2222", "9876",
        "4111-1111-1111-1111", "5555",
        "4000-0000-0000-0002", "0000"
    );

    public ValidationResult validate() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();
        String pin = request.pin();

        DemoUtil.simulateNetworkDelay(400);

        String correctPin = cardPins.get(cardNumber);

        if (pin == null || pin.trim().isEmpty()) {
            return ValidationResult.failure("PIN Validation: PIN is required");
        }

        if (correctPin == null) {
            return ValidationResult.failure("PIN Validation: PIN information not found for card");
        }

        if (!correctPin.equals(pin)) {
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }

        return ValidationResult.success("PIN Validation: Validation successful");
    }

}
