package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.services.PinValidationService;

public class ScopedPinValidationService extends PinValidationService implements ScopedValidationService {

    public ValidationResult validate() {
        return super.validate(ScopedPaymentProcessor.TRANSACTION_REQUEST.get(), ScopedPaymentProcessor.CARD.get());
    }

}
