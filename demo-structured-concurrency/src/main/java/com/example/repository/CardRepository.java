package com.example.repository;

import com.example.fixtures.DemoCards;
import com.example.model.Card;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CardRepository {
    private static final Logger logger = LogManager.getLogger(CardRepository.class);
    private final ConcurrentHashMap<String, Card> cards = new ConcurrentHashMap<>();

    public CardRepository() {
        initializeDemoCards();
    }

    private void initializeDemoCards() {
        // Initialize with demo cards from DemoCards fixture
        for (Card card : DemoCards.getAllCards()) {
            cards.put(card.cardNumber(), card);
        }
        logger.info("CardRepository initialized with {} demo cards", cards.size());
    }

    public Optional<Card> findByCardNumber(String cardNumber) {
        return Optional.ofNullable(cards.get(cardNumber));
    }

    public List<Card> findAll() {
        return cards.values().stream().toList();
    }

    public Card save(Card card) {
        cards.put(card.cardNumber(), card);
        logger.info("Card saved: {}", card.cardNumber());
        return card;
    }

    public void delete(String cardNumber) {
        cards.remove(cardNumber);
        logger.info("Card deleted: {}", cardNumber);
    }

    public Card clone(String cardNumber, String newCardNumber) {
        Optional<Card> original = findByCardNumber(cardNumber);
        if (original.isEmpty()) {
            throw new IllegalArgumentException("Card not found: " + cardNumber);
        }

        Card originalCard = original.get();
        Card clonedCard = new Card(
            newCardNumber,
            originalCard.expirationDate(),
            originalCard.pin(),
            originalCard.balance(),
            originalCard.description()
        );

        save(clonedCard);
        logger.info("Card cloned: {} → {}", cardNumber, newCardNumber);
        return clonedCard;
    }

    public boolean exists(String cardNumber) {
        return cards.containsKey(cardNumber);
    }

}
