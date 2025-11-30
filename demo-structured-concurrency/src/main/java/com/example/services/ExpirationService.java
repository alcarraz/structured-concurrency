package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@ApplicationScoped
public class ExpirationService implements ValidationService {

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(200);

        String expirationDate = request.expirationDate();

        try {
            // Parse MMYY format (e.g., "1225" for December 2025)
            if (expirationDate.length() != 4) {
                return ValidationResult.failure("Expiration Check: Invalid date format");
            }

            String month = expirationDate.substring(0, 2);
            String year = "20" + expirationDate.substring(2, 4);
            YearMonth cardExpiry = YearMonth.parse(year + "-" + month, DateTimeFormatter.ofPattern("yyyy-MM"));
            YearMonth currentMonth = YearMonth.now();

            if (cardExpiry.isBefore(currentMonth)) {
                return ValidationResult.failure("Expiration Check: Card expired");
            }

            return ValidationResult.success("Expiration Check: Validation successful");

        } catch (DateTimeParseException e) {
            return ValidationResult.failure("Expiration Check: Invalid date format");
        }
    }

}
