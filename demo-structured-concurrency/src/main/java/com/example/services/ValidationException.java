package com.example.services;

import com.example.model.ValidationResult;

/**
 * Custom exception for validation failures in the payment processing system.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

}
