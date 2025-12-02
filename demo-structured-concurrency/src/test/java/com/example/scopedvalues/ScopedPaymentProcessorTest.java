package com.example.scopedvalues;

import com.example.BaseProcessorTest;
import com.example.constants.ServiceDelays;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standalone JUnit test for ScopedPaymentProcessor.
 * Unlike other processor tests, this doesn't use @QuarkusTest because
 * ScopedPaymentProcessor and its services are manually instantiated.
 */
class ScopedPaymentProcessorTest extends BaseProcessorTest {

    private ScopedPaymentProcessor processor;

    @BeforeEach
    void setUp() {
        // Manually instantiate processor with scoped services (like the demo does)
        processor = new ScopedPaymentProcessor(
            new ScopedCardValidationService(new CardRepository()),
            new ScopedBalanceService(new CardRepository()),
            new ScopedExpirationService(),
            new ScopedPinValidationService(),
            new ScopedMerchantValidationService()
        );
    }

    @Test
    @DisplayName("Success with scoped values completes in expected time (~700ms)")
    void testSuccessTiming() throws Exception {
        TransactionRequest request = createValidRequest();

        TransactionResult result = processor.processTransaction(request);

        assertTrue(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME,
            "Scoped values success"
        );
    }

    @Test
    @DisplayName("Scoped context propagates correctly through parallel validations")
    void testScopedContextPropagation() throws Exception {
        TransactionRequest request = createValidRequest();

        // If scoped values aren't propagating, services would throw exceptions
        TransactionResult result = processor.processTransaction(request);

        assertTrue(result.success(), "Scoped values should propagate to all validations");
    }

    @Test
    @DisplayName("Expired card fails fast with scoped values (~300ms)")
    void testExpiredCardWithScopedValues() throws Exception {
        TransactionRequest request = createExpiredCardRequest();

        TransactionResult result = processor.processTransaction(request);

        assertFalse(result.success());
        assertTrue(result.message().contains("expired"));
        // Scoped processor throws exceptions for fail-fast behavior
        assertTimingWithinRange(
            result.processingTimeMs(),
            ServiceDelays.EXPECTED_EXPIRED_CARD_FAIL_FAST,  // Fails fast
            "Scoped values expired card (fail-fast)"
        );
    }
}
