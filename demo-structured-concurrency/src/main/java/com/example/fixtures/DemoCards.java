package com.example.fixtures;

import com.example.model.Card;
import java.math.BigDecimal;

/**
 * Centralized demo card definitions used across all demos and tests.
 * These cards are initialized in CardRepository and referenced by demo classes.
 */
public final class DemoCards {

    private DemoCards() {} // Prevent instantiation

    // Card 1: Valid card with high balance for success scenarios
    public static final String VALID_CARD_NUMBER = "4532-1234-5678-9012";
    public static final String VALID_CARD_EXPIRATION = "1225"; // Dec 2025
    public static final String VALID_CARD_PIN = "1234";
    public static final BigDecimal VALID_CARD_BALANCE = new BigDecimal("5000.00");

    public static final Card VALID_CARD = new Card(
        VALID_CARD_NUMBER,
        VALID_CARD_EXPIRATION,
        VALID_CARD_PIN,
        VALID_CARD_BALANCE,
        "Valid card for success scenarios"
    );

    // Card 2: Card with low balance for insufficient balance scenarios
    public static final String LOW_BALANCE_CARD_NUMBER = "9876-5432-1098-7654";
    public static final String LOW_BALANCE_CARD_EXPIRATION = "1225"; // Dec 2025
    public static final String LOW_BALANCE_CARD_PIN = "5678";
    public static final BigDecimal LOW_BALANCE_CARD_BALANCE = new BigDecimal("500.00");

    public static final Card LOW_BALANCE_CARD = new Card(
        LOW_BALANCE_CARD_NUMBER,
        LOW_BALANCE_CARD_EXPIRATION,
        LOW_BALANCE_CARD_PIN,
        LOW_BALANCE_CARD_BALANCE,
        "Low balance card"
    );

    // Card 3: Expired card for fail-fast scenarios
    public static final String EXPIRED_CARD_NUMBER = "5555-4444-3333-2222";
    public static final String EXPIRED_CARD_EXPIRATION = "1223"; // Dec 2023 (expired)
    public static final String EXPIRED_CARD_PIN = "9876";
    public static final BigDecimal EXPIRED_CARD_BALANCE = new BigDecimal("1000.00");

    public static final Card EXPIRED_CARD = new Card(
        EXPIRED_CARD_NUMBER,
        EXPIRED_CARD_EXPIRATION,
        EXPIRED_CARD_PIN,
        EXPIRED_CARD_BALANCE,
        "Expired card for fail-fast demos"
    );

    // Card 4: Card for ScopedValues demo
    public static final String SCOPED_CARD_NUMBER = "4111-1111-1111-1111";
    public static final String SCOPED_CARD_EXPIRATION = "1225"; // Dec 2025
    public static final String SCOPED_CARD_PIN = "5555";
    public static final BigDecimal SCOPED_CARD_BALANCE = new BigDecimal("2000.00");

    public static final Card SCOPED_CARD = new Card(
        SCOPED_CARD_NUMBER,
        SCOPED_CARD_EXPIRATION,
        SCOPED_CARD_PIN,
        SCOPED_CARD_BALANCE,
        "Card for ScopedValues demo"
    );

    // Card 5: High balance card for balance locking demos
    public static final String LOCKING_CARD_NUMBER = "1234-5678-9012-3456";
    public static final String LOCKING_CARD_EXPIRATION = "1225"; // Dec 2025
    public static final String LOCKING_CARD_PIN = "1234";
    public static final BigDecimal LOCKING_CARD_BALANCE = new BigDecimal("5000.00");

    public static final Card LOCKING_CARD = new Card(
        LOCKING_CARD_NUMBER,
        LOCKING_CARD_EXPIRATION,
        LOCKING_CARD_PIN,
        LOCKING_CARD_BALANCE,
        "Card for balance locking demos"
    );

    /**
     * Returns all demo cards as an array for easy initialization.
     */
    public static Card[] getAllCards() {
        return new Card[] {
            VALID_CARD,
            LOW_BALANCE_CARD,
            EXPIRED_CARD,
            SCOPED_CARD,
            LOCKING_CARD
        };
    }
}
