package com.example.scopedvalues;

import com.example.model.CardValidationResult;
import com.example.repository.CardRepository;
import com.example.services.CardValidationService;

public class ScopedCardValidationService extends CardValidationService {

    public ScopedCardValidationService(CardRepository cardRepository) {
        super(cardRepository);
    }

    public CardValidationResult validate() {
        return super.validate(ScopedPaymentProcessor.TRANSACTION_REQUEST.get());
    }

}
