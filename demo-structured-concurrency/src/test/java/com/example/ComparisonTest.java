package com.example;

import com.example.constants.ServiceDelays;
import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ComparisonTest extends BaseProcessorTest {

    @Inject
    BasicReactivePaymentProcessor reactive;

    @Inject
    StructuredPaymentProcessor structured;

    @Inject
    FailFastStructuredPaymentProcessor failFast;

    @Test
    @DisplayName("Success scenarios have similar timing across all processors")
    void testSuccessTimingParity() throws InterruptedException {
        TransactionRequest request = createValidRequest();

        TransactionResult reactiveResult = reactive.processTransaction(request).join();
        TransactionResult structuredResult = structured.processTransaction(request);
        TransactionResult failFastResult = failFast.processTransaction(request);

        // All should be around 700ms
        assertTimingWithinRange(reactiveResult.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME, "Reactive success");
        assertTimingWithinRange(structuredResult.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME, "Structured success");
        assertTimingWithinRange(failFastResult.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME, "Fail-fast success");
    }

    @Test
    @DisplayName("Fail-fast is significantly faster than reactive on expired card")
    void testFailFastAdvantage() throws InterruptedException {
        TransactionRequest request = createExpiredCardRequest();

        TransactionResult reactiveResult = reactive.processTransaction(request).join();
        TransactionResult failFastResult = failFast.processTransaction(request);

        // Reactive waits for all (~700ms)
        assertTimingWithinRange(reactiveResult.processingTimeMs(),
            ServiceDelays.EXPECTED_SUCCESS_TIME, "Reactive expired (awaits all)");

        // Fail-fast terminates early (~300ms)
        assertTimingWithinRange(failFastResult.processingTimeMs(),
            ServiceDelays.EXPECTED_EXPIRED_CARD_FAIL_FAST, "Fail-fast expired");

        // Verify speedup
        long speedup = reactiveResult.processingTimeMs() - failFastResult.processingTimeMs();
        assertTrue(speedup > 300,
            String.format("Fail-fast should be >300ms faster, was %dms faster", speedup));
    }
}
