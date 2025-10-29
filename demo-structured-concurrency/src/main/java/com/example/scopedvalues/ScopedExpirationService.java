package com.example.scopedvalues;

import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ScopedExpirationService {

    public ValidationResult validate(String cardNumber, String expirationDate) {
        auditLog("Starting expiration validation for card: " + cardNumber.substring(cardNumber.length() - 4));

        DemoUtil.simulateNetworkDelay(200);

        try {
            if (expirationDate.length() != 4) {
                auditLog("Expiration validation failed: Invalid date format");
                return ValidationResult.failure("Expiration Check: Invalid date format");
            }

            String year = "20" + expirationDate.substring(0, 2);
            String month = expirationDate.substring(2, 4);
            YearMonth cardExpiry = YearMonth.parse(year + "-" + month, DateTimeFormatter.ofPattern("yyyy-MM"));
            YearMonth currentMonth = YearMonth.now();

            if (cardExpiry.isBefore(currentMonth)) {
                auditLog("Expiration validation failed: Card expired");
                return ValidationResult.failure("Expiration Check: Card expired");
            }

            auditLog("Expiration validation successful");
            return ValidationResult.success("Expiration Check: Validation successful");
        } catch (Exception e) {
            auditLog("Expiration validation failed: Invalid expiration date format");
            return ValidationResult.failure("Expiration Check: Invalid expiration date format");
        }
    }

    private void auditLog(String message) {
        RequestContext context = ScopedPaymentProcessor.REQUEST_CONTEXT.get();
        System.out.println("📅 EXPIRATION [" + context.correlationId() + "] " + message);
    }

}
