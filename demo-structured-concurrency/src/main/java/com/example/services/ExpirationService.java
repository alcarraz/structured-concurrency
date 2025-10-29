package com.example.services;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ExpirationService {

    public ValidationResult validate(String cardNumber, String expirationDate) {
        DemoUtil.simulateNetworkDelay(200);

        try {
            // Parse YYMM format (e.g., "1225" for December 2025)
            if (expirationDate.length() != 4) {
                return ValidationResult.failure("Expiration Check: Invalid date format");
            }

            String year = "20" + expirationDate.substring(0, 2);
            String month = expirationDate.substring(2, 4);
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