package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
    String customerId,
    String cardNumber,
    String expirationDate, // YYMM format (e.g., "1225" for December 2025)
    String pin,
    BigDecimal amount,
    String merchant,
    LocalDateTime timestamp
) {
    public TransactionRequest(String customerId, String cardNumber, String expirationDate, String pin, BigDecimal amount, String merchant) {
        this(customerId, cardNumber, expirationDate, pin, amount, merchant, LocalDateTime.now());
    }

}