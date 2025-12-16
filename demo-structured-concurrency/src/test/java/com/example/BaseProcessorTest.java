package com.example;

import com.example.constants.ServiceDelays;
import com.example.model.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static com.example.fixtures.DemoCards.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for processor timing tests with common utilities.
 */
public abstract class BaseProcessorTest {

    // Acceptable variance for timing assertions
    protected static final long TIMING_TOLERANCE_MS = 75L;
    protected static final long EXPECTED_INVALID_PIN_FAIL_FAST =
        ServiceDelays.CARD_VALIDATION_DELAY + ServiceDelays.PIN_VALIDATION_DELAY;  // ~400ms
    protected static final long EXPECTED_EXPIRED_CARD_FAIL_FAST =
        ServiceDelays.CARD_VALIDATION_DELAY + ServiceDelays.EXPIRATION_VALIDATION_DELAY;  // ~300ms
    // Calculated expected times for common scenarios (long for timing comparisons)
    protected static final long EXPECTED_SUCCESS_TIME =
        Math.max(ServiceDelays.MERCHANT_VALIDATION_DELAY,
                 ServiceDelays.CARD_VALIDATION_DELAY + ServiceDelays.BALANCE_VALIDATION_DELAY);  // ~700ms
    protected long startTime;

    @BeforeEach
    void resetTimer() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Assert that actual timing is within tolerance of expected.
     */
    protected void assertTimingWithinRange(long actualMs, long expectedMs, String scenario) {
        long lowerBound = expectedMs - TIMING_TOLERANCE_MS;
        long upperBound = expectedMs + TIMING_TOLERANCE_MS;

        assertTrue(actualMs >= lowerBound && actualMs <= upperBound,
            String.format("%s: Expected %dms (±%dms), but was %dms",
                scenario, expectedMs, TIMING_TOLERANCE_MS, actualMs));
    }

    /**
     * Create valid transaction request for success scenarios.
     */
    protected TransactionRequest createValidRequest() {
        return new TransactionRequest(
            VALID_CARD_NUMBER,
            VALID_CARD_EXPIRATION,
            VALID_CARD_PIN,
            new BigDecimal("100.00"),
            "Test Merchant"
        );
    }

    /**
     * Create expired card request for fail-fast scenarios.
     */
    protected TransactionRequest createExpiredCardRequest() {
        return new TransactionRequest(
            EXPIRED_CARD_NUMBER,
            EXPIRED_CARD_EXPIRATION,
            EXPIRED_CARD_PIN,
            new BigDecimal("75.00"),
            "Test Merchant"
        );
    }

    /**
     * Create invalid PIN request for fail-fast scenarios.
     */
    protected TransactionRequest createInvalidPinRequest() {
        return new TransactionRequest(
            VALID_CARD_NUMBER,
            VALID_CARD_EXPIRATION,
            "9999",  // Invalid PIN
            new BigDecimal("50.00"),
            "Test Merchant"
        );
    }
}
