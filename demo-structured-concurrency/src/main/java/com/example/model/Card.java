package com.example.model;

import java.math.BigDecimal;

public record Card(
    String cardNumber,
    String expirationDate, // MMYY format
    String pin,
    BigDecimal balance,
    String description
) {
    // Compact constructor for validation
    // Note: cardNumber CAN be null during PUT endpoint deserialization
    // (it's provided via path parameter, not request body)
    public Card {
        if (cardNumber != null && cardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number cannot be blank");
        }
    }
}
