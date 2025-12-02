package com.example.scopedvalues;

import com.example.model.ValidationResult;

/**
 * Interface for validation services that access context via ScopedValues.
 * Services implementing this interface read TransactionRequest and/or Card
 * from scoped values without requiring explicit parameter passing.
 */
public interface ScopedValidationService {
    /**
     * Validates using context from ScopedValues.
     * @return ValidationResult indicating success or failure
     */
    ValidationResult validate();
}
