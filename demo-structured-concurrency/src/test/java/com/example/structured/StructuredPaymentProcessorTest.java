package com.example.structured;

import com.example.BaseProcessorTest;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.repository.CardRepository;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructuredPaymentProcessorTest extends BaseProcessorTest {
    CardRepository cardRepository = new CardRepository();
    BalanceService balanceService = new BalanceService(cardRepository);
    CardValidationService cardValidationService = new CardValidationService(cardRepository);
    ExpirationService expirationService = new ExpirationService();
    PinValidationService pinValidationService = new PinValidationService();
    MerchantValidationService merchantValidationService = new MerchantValidationService();

    StructuredPaymentProcessor processor = new StructuredPaymentProcessor(
            balanceService,
            cardValidationService,
            expirationService,
            pinValidationService,
            merchantValidationService

    );

    @Test
    @DisplayName("Success scenario completes in expected time (~700ms)")
    void testSuccessTiming() throws InterruptedException {
        TransactionRequest request = createValidRequest();

        TransactionResult result = processor.processTransaction(request);

        assertTrue(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME,
            "Structured success"
        );
    }

    @Test
    @DisplayName("Expired card waits for all validations (~700ms)")
    void testExpiredCardAwaitsAll() throws InterruptedException {
        TransactionRequest request = createExpiredCardRequest();

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME,  // Awaits all
            "Structured expired card (await-all)"
        );
    }

    @Test
    @DisplayName("Invalid PIN waits for all validations (~700ms)")
    void testInvalidPinAwaitsAll() throws InterruptedException {
        TransactionRequest request = createInvalidPinRequest();

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME,  // Awaits all
            "Structured invalid PIN (await-all)"
        );
    }
}
