package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.services.ExpirationService;

public class ScopedExpirationService extends ExpirationService implements ScopedValidationService {

    public ValidationResult validate() {
        return super.validate(ScopedPaymentProcessor.TRANSACTION_REQUEST.get(), ScopedPaymentProcessor.CARD.get());
    }
}
