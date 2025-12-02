package com.example.scopedvalues;

import com.example.model.Card;
import com.example.model.CardValidationResult;
import com.example.model.TransactionRequest;
import com.example.repository.CardRepository;
import com.example.utils.DemoUtil;

import java.util.Optional;

public class ScopedCardValidationService {

    private final CardRepository cardRepository;

    public ScopedCardValidationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public CardValidationResult validate() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String cardNumber = request.cardNumber();

        DemoUtil.simulateNetworkDelay(300);

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
