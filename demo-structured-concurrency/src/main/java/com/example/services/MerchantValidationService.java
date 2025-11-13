package com.example.services;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

public class MerchantValidationService {

    public ValidationResult validate(String merchant) {
        DemoUtil.simulateNetworkDelay(300);

        // Simple check: fail if merchant contains "BLOCKED" for demo purposes
        if (merchant.toUpperCase().contains("BLOCKED")) {
            return ValidationResult.failure("Merchant Validation: Merchant is blocked");
        }
        return ValidationResult.success("Merchant Validation: Validation successful");
    }

}
