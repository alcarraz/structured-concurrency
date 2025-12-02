package com.example.model;

/**
 * Result of card validation using sealed interface pattern for type-safe success/failure handling.
 * Enables pattern matching and exhaustiveness checking.
 */
public sealed interface CardValidationResult {

    static boolean isFailure(CardValidationResult result) {
        return result instanceof Failure;
    }
    /**
     * Successful validation with the validated Card.
     *
     * @param card The validated card from the repository
     */
    record Success(Card card) implements CardValidationResult {}

    /**
     * Failed validation with error message.
     *
     * @param message Failure reason
     */
    record Failure(String message) implements CardValidationResult {}

    // Static factory methods for backward compatibility
    static CardValidationResult success(Card card) {
        return new Success(card);
    }

    static CardValidationResult failure(String message) {
        return new Failure(message);
    }
}
