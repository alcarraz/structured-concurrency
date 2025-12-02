package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.repository.CardRepository;
import com.example.services.BalanceService;

// extends BalanceService only for simplification of code for the demo,
// it should be a complete new implementation or using composition.
public class ScopedBalanceService extends BalanceService implements ScopedValidationService {

    public ScopedBalanceService(CardRepository cardRepository) {
        super(cardRepository);
    }

    public ValidationResult validate() {
        return super.validate(ScopedPaymentProcessor.TRANSACTION_REQUEST.get(), ScopedPaymentProcessor.CARD.get());
    }

    public void transfer() {
        super.transfer(ScopedPaymentProcessor.TRANSACTION_REQUEST.get(), ScopedPaymentProcessor.CARD.get());
    }

}
