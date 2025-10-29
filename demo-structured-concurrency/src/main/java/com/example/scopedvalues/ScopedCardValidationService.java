package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class ScopedCardValidationService {

    public ValidationResult validate(String cardNumber) {
        auditLog("Starting card validation for: " + cardNumber);

        DemoUtil.simulateNetworkDelay(300);

        // Simple check: fail if card contains "0000" for demo purposes
        if (cardNumber.contains("0000")) {
            auditLog("Card validation failed: Invalid card");
            return ValidationResult.failure("Card Validation: Invalid card");
        }

        auditLog("Card validation successful");
        return ValidationResult.success("Card Validation: Validation successful");
    }


    private void auditLog(String message) {
        RequestContext context = ScopedPaymentProcessor.REQUEST_CONTEXT.get();
        System.out.println("💳 CARD [" + context.correlationId() + "] " + message);
    }

}
