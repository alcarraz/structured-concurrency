package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;

/**
 * Common interface for all validation services.
 * Each service validates a specific aspect of the transaction.
 */
public interface ValidationService {

    /**
     * Validates the transaction request.
     *
     * @param request The transaction request to validate
     * @return ValidationResult indicating success or failure
     */
    ValidationResult validate(TransactionRequest request);
}
