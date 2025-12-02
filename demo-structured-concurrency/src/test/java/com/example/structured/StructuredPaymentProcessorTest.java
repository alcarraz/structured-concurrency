package com.example.structured;

import com.example.BaseProcessorTest;
import com.example.constants.ServiceDelays;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StructuredPaymentProcessorTest extends BaseProcessorTest {

    @Inject
    StructuredPaymentProcessor processor;

    @Test
    @DisplayName("Success scenario completes in expected time (~700ms)")
    void testSuccessTiming() throws InterruptedException {
        TransactionRequest request = createValidRequest();

        TransactionResult result = processor.processTransaction(request);

        assertTrue(result.success());
        assertTimingWithinRange(
            result.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME,
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
            ServiceDelays.EXPECTED_SUCCESS_TIME,  // Awaits all
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
            ServiceDelays.EXPECTED_SUCCESS_TIME,  // Awaits all
            "Structured invalid PIN (await-all)"
        );
    }
}
