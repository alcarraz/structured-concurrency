package com.example.repository;

import com.example.model.Card;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class CardRepository {
    private static final Logger logger = LogManager.getLogger(CardRepository.class);
    private final ConcurrentHashMap<String, Card> cards = new ConcurrentHashMap<>();
    private final AtomicInteger cardNumberCounter = new AtomicInteger(1000);

    public CardRepository() {
        initializeDemoCards();
    }

    private void initializeDemoCards() {
        cards.put("1234-5678-9012-3456", new Card(
            "1234-5678-9012-3456",
            "2512",
            "1234",
            new BigDecimal("5000"),
            "Valid card for success scenarios"
        ));

        cards.put("9876-5432-1098-7654", new Card(
            "9876-5432-1098-7654",
            "2512",
            "5678",
            new BigDecimal("500"),
            "Low balance card"
        ));

        cards.put("1111-2222-3333-4444", new Card(
            "1111-2222-3333-4444",
            "1220",
            "9999",
            new BigDecimal("100"),
            "Expired card for fail-fast demos"
        ));

        logger.info("CardRepository initialized with {} demo cards", cards.size());
    }

    public Optional<Card> findByCardNumber(String cardNumber) {
        return Optional.ofNullable(cards.get(cardNumber));
    }

    public List<Card> findAll() {
        return cards.values().stream().toList();
    }

    public Card save(Card card) {
        cards.put(card.getCardNumber(), card);
        logger.info("Card saved: {}", card.getCardNumber());
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
            originalCard.getExpirationDate(),
            originalCard.getPin(),
            originalCard.getBalance(),
            originalCard.getDescription()
        );

        save(clonedCard);
        logger.info("Card cloned: {} → {}", cardNumber, newCardNumber);
        return clonedCard;
    }

    public boolean exists(String cardNumber) {
        return cards.containsKey(cardNumber);
    }

}
