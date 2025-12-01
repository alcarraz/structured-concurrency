package com.example.services;

import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.repository.CardRepository;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class PinValidationService implements ValidationService {

    private final CardRepository cardRepository;

    @Inject
    public PinValidationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(400);

        String requestPin = request.pin();

        // Lookup card in repository
        Optional<Card> cardOpt = cardRepository.findByCardNumber(request.cardNumber());
        if (cardOpt.isEmpty()) {
            return ValidationResult.failure("PIN Validation: Card not found");
        }

        Card card = cardOpt.get();

        // Compare request PIN vs repository PIN
        if (!requestPin.equals(card.pin())) {
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }

        return ValidationResult.success("PIN Validation: Validation successful");
    }

}
