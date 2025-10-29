package com.example.model;

public record ValidationResult(
    boolean success,
    String message
) {
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message);
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}
