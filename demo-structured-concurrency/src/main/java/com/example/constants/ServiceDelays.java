package com.example.constants;

/**
 * Public constants defining simulated network delays for each validation service.
 * Used in both production code and tests to calculate expected timing ranges.
 */
public final class ServiceDelays {
    private ServiceDelays() {} // Utility class

    // Individual service delays (milliseconds)
    public static final int CARD_VALIDATION_DELAY = 100;
    public static final int EXPIRATION_VALIDATION_DELAY = 200;
    public static final int PIN_VALIDATION_DELAY = 300;
    public static final int MERCHANT_VALIDATION_DELAY = 500;
    public static final int BALANCE_VALIDATION_DELAY = 600;  // Slowest

    // Calculated expected times for common scenarios (long for timing comparisons)
    public static final long EXPECTED_SUCCESS_TIME =
        Math.max(MERCHANT_VALIDATION_DELAY,
                 CARD_VALIDATION_DELAY + BALANCE_VALIDATION_DELAY);  // ~700ms

    public static final long EXPECTED_EXPIRED_CARD_FAIL_FAST =
        CARD_VALIDATION_DELAY + EXPIRATION_VALIDATION_DELAY;  // ~300ms

    public static final long EXPECTED_INVALID_PIN_FAIL_FAST =
        CARD_VALIDATION_DELAY + PIN_VALIDATION_DELAY;  // ~400ms

    // Acceptable variance for timing assertions (±50ms accounts for JVM/system overhead)
    public static final long TIMING_TOLERANCE_MS = 50L;
}
