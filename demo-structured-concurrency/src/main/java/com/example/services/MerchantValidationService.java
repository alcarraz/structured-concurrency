package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class MerchantValidationService implements ValidationService {

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(300);

        String merchant = request.merchant();

        // Simple check: fail if merchant contains "BLOCKED" for demo purposes
        if (merchant.toUpperCase().contains("BLOCKED")) {
            return ValidationResult.failure("Merchant Validation: Merchant is blocked");
        }
        return ValidationResult.success("Merchant Validation: Validation successful");
    }

}
