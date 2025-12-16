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

}
