package com.example.structured;

import com.example.BaseProcessorTest;
import com.example.constants.ServiceDelays;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.fixtures.DemoCards.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FailFastStructuredPaymentProcessorTest extends BaseProcessorTest {

    @Inject
    FailFastStructuredPaymentProcessor processor;

    @Test
    @DisplayName("Success scenario completes in expected time (~700ms)")
    void testSuccessTiming() throws InterruptedException {
        TransactionRequest request = createValidRequest();

        TransactionResult result = processor.processTransaction(request);

        assertTrue(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME,
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
            ServiceDelays.EXPECTED_EXPIRED_CARD_FAIL_FAST,
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
            ServiceDelays.EXPECTED_INVALID_PIN_FAIL_FAST,
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
    }
}
