package com.example.scopedvalues;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ScopedExpirationService {

    public ValidationResult validate() {
        TransactionRequest request = ScopedPaymentProcessor.TRANSACTION_REQUEST.get();
        String expirationDate = request.expirationDate();

        DemoUtil.simulateNetworkDelay(200);

        try {
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
        } catch (Exception e) {
            return ValidationResult.failure("Expiration Check: Invalid expiration date format");
        }
    }

}
