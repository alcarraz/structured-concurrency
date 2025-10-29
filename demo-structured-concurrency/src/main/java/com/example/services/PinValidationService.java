package com.example.services;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class PinValidationService {

    public ValidationResult validate(String ignored, String pin) {
        DemoUtil.simulateNetworkDelay(400);

        // Simple check: fail if PIN is "0000" for demo purposes
        if ("0000".equals(pin)) {
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }
        return ValidationResult.success("PIN Validation: Validation successful");
    }

}
