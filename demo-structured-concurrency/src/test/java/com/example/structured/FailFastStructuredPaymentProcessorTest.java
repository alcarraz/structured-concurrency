package com.example.structured;

import com.example.BaseProcessorTest;
import com.example.constants.ServiceDelays;
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

import java.math.BigDecimal;

import static com.example.fixtures.DemoCards.*;
import static org.junit.jupiter.api.Assertions.*;

class FailFastStructuredPaymentProcessorTest extends BaseProcessorTest {

    CardRepository cardRepository = new CardRepository();
    BalanceService balanceService = new BalanceService(cardRepository);
    CardValidationService cardValidationService = new CardValidationService(cardRepository);
    ExpirationService expirationService = new ExpirationService();
    PinValidationService pinValidationService = new PinValidationService();
    MerchantValidationService merchantValidationService = new MerchantValidationService();

    FailFastStructuredPaymentProcessor processor = new FailFastStructuredPaymentProcessor(
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
            "Fail-fast success"
        );
    }

    @Test
    @DisplayName("Expired card fails fast (~300ms)")
    void testExpiredCardFailsFast() throws InterruptedException {
        TransactionRequest request = createExpiredCardRequest();

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTrue(result.message().contains("expired"));
        assertTimingWithinRange(
            result.processingTimeMs(),
            BaseProcessorTest.EXPECTED_EXPIRED_CARD_FAIL_FAST,
            "Fail-fast expired card"
        );
    }

    @Test
    @DisplayName("Invalid PIN fails fast (~400ms)")
    void testInvalidPinFailsFast() throws InterruptedException {
        TransactionRequest request = createInvalidPinRequest();

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTrue(result.message().contains("PIN"));
        assertTimingWithinRange(
            result.processingTimeMs(),
            BaseProcessorTest.EXPECTED_INVALID_PIN_FAIL_FAST,
            "Fail-fast invalid PIN"
        );
    }

    @Test
    @DisplayName("Merchant validation failure propagates correctly")
    void testMerchantFailure() throws InterruptedException {
        TransactionRequest request = new TransactionRequest(
            VALID_CARD_NUMBER,
            VALID_CARD_EXPIRATION,
            VALID_CARD_PIN,
            new BigDecimal("100.00"),
            "BLOCKED_MERCHANT"  // Triggers merchant failure
        );

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTrue(result.message().contains("Merchant"));
        assertTimingWithinRange(
            result.processingTimeMs(),
            ServiceDelays.MERCHANT_VALIDATION_DELAY,  // 500ms
            "Fail-fast merchant validation"
        );
    }
}
