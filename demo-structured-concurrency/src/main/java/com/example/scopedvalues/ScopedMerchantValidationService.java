package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.services.MerchantValidationService;

public class ScopedMerchantValidationService extends MerchantValidationService implements ScopedValidationService{
    @Override
    public ValidationResult validate() {
        return super.validate(ScopedPaymentProcessor.TRANSACTION_REQUEST.get());
    }
}
