package com.example.services;

import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.repository.CardRepository;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@ApplicationScoped
public class ExpirationService implements ValidationService {

    private static final Logger logger = LogManager.getLogger();
    private final CardRepository cardRepository;

    @Inject
    public ExpirationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }
    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(200);

        String expirationDate = request.expirationDate();

        try {
            // Parse MMYY format (e.g., "1225" for December 2025)
            if (expirationDate.length() != 4) {
                return ValidationResult.failure("Expiration Check: Invalid date format");
            }

            YearMonth requestExpiry = YearMonth.parse(expirationDate, DateTimeFormatter.ofPattern("MMyy"));

            // Lookup card in repository
            Optional<Card> cardOpt = cardRepository.findByCardNumber(request.cardNumber());
            if (cardOpt.isEmpty()) {
                return ValidationResult.failure("Expiration Check: Card not found");
            }

            Card card = cardOpt.get();

            // Parse card's expiration date from repository
            YearMonth cardExpiry;
            try {
                if (card.expirationDate().length() != 4) {
                    return ValidationResult.failure("Expiration Check: Invalid date format in card data");
                }
                cardExpiry = YearMonth.parse(card.expirationDate(), DateTimeFormatter.ofPattern("MMyy"));
            } catch (DateTimeParseException e) {
                logger.error("Invalid date format in card repository data", e);
                return ValidationResult.failure("Expiration Check: Invalid date format in card data");
            }

            // Compare request date vs repository date
            if (!requestExpiry.equals(cardExpiry)) {
                return ValidationResult.failure("Expiration Check: Invalid expiration date");
            }

            // Check if card is expired
            YearMonth currentMonth = YearMonth.now();
            if (cardExpiry.isBefore(currentMonth)) {
                return ValidationResult.failure("Expiration Check: Card expired");
            }

            return ValidationResult.success("Expiration Check: Validation successful");

        } catch (DateTimeParseException e) {
            logger.error("Invalid date format", e);
            return ValidationResult.failure("Expiration Check: Invalid date format");
        }
    }

}
