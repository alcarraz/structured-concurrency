package com.example.services;

import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@ApplicationScoped
public class ExpirationService implements CardAwareValidationService {

    private static final Logger logger = LogManager.getLogger();

    @Inject
    public ExpirationService() {
        // No dependencies
    }

    @Override
    public ValidationResult validate(TransactionRequest request, @NotNull Card card) {
        DemoUtil.simulateNetworkDelay(200);

        String expirationDate = request.expirationDate();

        try {
            if (!request.expirationDate().equals(card.expirationDate())) return new ValidationResult.Failure("Invalid card data");
            YearMonth expiry = YearMonth.parse(expirationDate, DateTimeFormatter.ofPattern("MMyy"));

            // Check if card is expired
            YearMonth currentMonth = YearMonth.now();
            if (expiry.isBefore(currentMonth)) {
                return ValidationResult.failure("Expiration Check: Card expired");
            }

            return ValidationResult.success();

        } catch (DateTimeParseException e) {
            logger.error("Invalid date format", e);
            return ValidationResult.failure("Expiration Check: Invalid date format");
        }
    }

}
