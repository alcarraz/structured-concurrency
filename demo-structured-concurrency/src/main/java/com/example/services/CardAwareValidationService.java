package com.example.services;

import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import jakarta.validation.constraints.NotNull;

/**
 * Validation service interface for services that receive a Card object
 * as context for their validation logic. This eliminates duplicate
 * repository lookups since the Card is retrieved once by CardValidationService.
 */
public interface CardAwareValidationService {

    /**
     * Validates the transaction request using the provided Card object.
     *
     * @param request The transaction request to validate
     * @param card The card object (required, never null)
     * @return ValidationResult indicating success or failure
     */
    ValidationResult validate(TransactionRequest request, @NotNull Card card);
}
