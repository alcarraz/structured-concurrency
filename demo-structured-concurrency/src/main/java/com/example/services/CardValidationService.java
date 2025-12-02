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
    private static final CardValidationResult.Failure NOT_FOUND = new CardValidationResult.Failure("Card Not Found");

    @Inject
    public CardValidationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public CardValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(100);

        String cardNumber = request.cardNumber();

        // Lookup card in repository
        return cardRepository.findByCardNumber(cardNumber)
                .<CardValidationResult>map(CardValidationResult.Success::new)
                .orElse(NOT_FOUND);
    }

}
