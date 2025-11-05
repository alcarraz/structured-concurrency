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

        auditLog("Starting PIN validation for card: " + cardNumber.substring(cardNumber.length() - 4));

        DemoUtil.simulateNetworkDelay(400);

        String correctPin = cardPins.get(cardNumber);

        if (pin == null || pin.trim().isEmpty()) {
            auditLog("PIN validation failed: PIN is required");
            return ValidationResult.failure("PIN Validation: PIN is required");
        }

        if (correctPin == null) {
            auditLog("PIN validation failed: PIN information not found for card");
            return ValidationResult.failure("PIN Validation: PIN information not found for card");
        }

        if (!correctPin.equals(pin)) {
            auditLog("PIN validation failed: Invalid PIN");
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }

        auditLog("PIN validation successful");
        return ValidationResult.success("PIN Validation: Validation successful");
    }

    private void auditLog(String message) {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        System.out.println("🔐 PIN [Customer: " + request.customerId() + "] " + message);
    }

}
