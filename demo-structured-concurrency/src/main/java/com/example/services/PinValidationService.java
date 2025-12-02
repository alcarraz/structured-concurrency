package com.example.services;

import com.example.constants.ServiceDelays;
import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

@ApplicationScoped
public class PinValidationService implements CardAwareValidationService {

    @Inject
    public PinValidationService() {
        // No dependencies
    }

    @Override
    public ValidationResult validate(TransactionRequest request, @NotNull Card card) {
        DemoUtil.simulateNetworkDelay(ServiceDelays.PIN_VALIDATION_DELAY);

        String requestPin = request.pin();

        // Compare request PIN vs card PIN
        if (!requestPin.equals(card.pin())) {
            return ValidationResult.failure("PIN Validation: Invalid PIN");
        }

        return ValidationResult.success();
    }

}
