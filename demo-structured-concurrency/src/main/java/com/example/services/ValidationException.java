package com.example.services;

import com.example.model.ValidationResult;

/**
 * Custom exception for validation failures in the payment processing system.
 */
public class ValidationException extends RuntimeException {

    final ValidationResult result;

    public ValidationException(ValidationResult result) {
        super(result.message());
        this.result = result; 
    }

    public ValidationResult getResult() {
        return result;
    }
}
