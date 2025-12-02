package com.example.services;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.repository.CardRepository;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class CardValidationService {  // NO LONGER implements ValidationService

    private final CardRepository cardRepository;

    @Inject
    public CardValidationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public CardValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(300);

        String cardNumber = request.cardNumber();

        // Simple validation: fail if card contains "0000" for demo purposes
        if (cardNumber.contains("0000")) {
            return CardValidationResult.failure("Card Validation: Invalid card");
        }

        // Lookup card in repository
        Optional<Card> cardOpt = cardRepository.findByCardNumber(cardNumber);
        if (cardOpt.isEmpty()) {
            throw new RuntimeException("Card Validation: Card not found");
        }

        return CardValidationResult.success(cardOpt.get());
    }

}
