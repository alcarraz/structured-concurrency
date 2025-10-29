package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResult(
    boolean success,
    String transactionId,
    BigDecimal amount,
    String message,
    LocalDateTime processedAt,
    long processingTimeMs
) {
    public static TransactionResult success(String transactionId, BigDecimal amount, long processingTimeMs) {
        return new TransactionResult(
            true,
            transactionId,
            amount,
            "Transaction processed successfully",
            LocalDateTime.now(),
            processingTimeMs
        );
    }

    public static TransactionResult failure(String message, long processingTimeMs) {
        return new TransactionResult(
            false,
            null,
            null,
            message,
            LocalDateTime.now(),
            processingTimeMs
        );
    }
}
