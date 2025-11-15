package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class PinValidationService implements ValidationService {

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(400);

        String pin = request.pin();

        // Simple check: fail if PIN is "0000" for demo purposes
        if ("0000".equals(pin)) {
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }
        return ValidationResult.success("PIN Validation: Validation successful");
    }

}
