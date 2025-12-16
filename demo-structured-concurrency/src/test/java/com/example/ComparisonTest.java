package com.example;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.repository.CardRepository;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonTest extends BaseProcessorTest {

    CardRepository cardRepository = new CardRepository();
    BalanceService balanceService = new BalanceService(cardRepository);
    CardValidationService cardValidationService = new CardValidationService(cardRepository);
    ExpirationService expirationService = new ExpirationService();
    PinValidationService pinValidationService = new PinValidationService();
    MerchantValidationService merchantValidationService = new MerchantValidationService();
    BasicReactivePaymentProcessor reactive = new BasicReactivePaymentProcessor(
            balanceService,
            cardValidationService,
            expirationService,
            pinValidationService,
            merchantValidationService
    );

    StructuredPaymentProcessor structured = new StructuredPaymentProcessor(
            balanceService,
            cardValidationService,
            expirationService,
            pinValidationService,
            merchantValidationService
 
    );

    FailFastStructuredPaymentProcessor failFast = new FailFastStructuredPaymentProcessor(
            balanceService,
            cardValidationService,
            expirationService,
            pinValidationService,
            merchantValidationService
    );

    @Test
    @DisplayName("Success scenarios have similar timing across all processors")
    void testSuccessTimingParity() throws InterruptedException {
        TransactionRequest request = createValidRequest();

        TransactionResult reactiveResult = reactive.processTransaction(request).join();
        TransactionResult structuredResult = structured.processTransaction(request);
        TransactionResult failFastResult = failFast.processTransaction(request);

        // All should be around 700ms
        assertTimingWithinRange(reactiveResult.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME, "Reactive success");
        assertTimingWithinRange(structuredResult.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME, "Structured success");
        assertTimingWithinRange(failFastResult.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME, "Fail-fast success");
    }

    @Test
    @DisplayName("Fail-fast is significantly faster than reactive on expired card")
    void testFailFastAdvantage() throws InterruptedException {
        TransactionRequest request = createExpiredCardRequest();

        TransactionResult reactiveResult = reactive.processTransaction(request).join();
        TransactionResult failFastResult = failFast.processTransaction(request);

        // Reactive waits for all (~700ms)
        assertTimingWithinRange(reactiveResult.processingTimeMs(),
            BaseProcessorTest.EXPECTED_SUCCESS_TIME, "Reactive expired (awaits all)");

        // Fail-fast terminates early (~300ms)
        assertTimingWithinRange(failFastResult.processingTimeMs(),
            BaseProcessorTest.EXPECTED_EXPIRED_CARD_FAIL_FAST, "Fail-fast expired");

        // Verify speedup
        long speedup = reactiveResult.processingTimeMs() - failFastResult.processingTimeMs();
        assertTrue(speedup > 300,
            String.format("Fail-fast should be >300ms faster, was %dms faster", speedup));
    }
}
