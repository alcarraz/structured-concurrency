package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.repository.CardRepository;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

import static com.example.fixtures.DemoCards.*;

/**
 * Early Failure Behavior Comparison Demo
 * <p>
 * This is the KILLER demo that shows the dramatic difference in failure handling
 * between Reactive and Structured Concurrency approaches. Structured concurrency
 * provides 50%+ faster failure response by automatically cancelling other tasks.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class CompareFailureDemo {
    private static final Logger logger = LogManager.getLogger(CompareFailureDemo.class);

    public void main() {
        // Create CardRepository first
        CardRepository cardRepository = new CardRepository();

        // Create services (passing cardRepository to BalanceService)
        BalanceService balanceService = new BalanceService(cardRepository);
        CardValidationService cardValidationService = new CardValidationService();
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        MerchantValidationService merchantValidationService = new MerchantValidationService();

        logger.info("💥 Running EARLY FAILURE BEHAVIOR COMPARISON Demo");
        logger.info("════════════════════════════════════════════════");
        logger.info("⚠️  Using EXPIRED CARD scenario to demonstrate early failure handling\n");

        // Use expired card request to trigger early failure
        TransactionRequest expiredCardRequest = new TransactionRequest(
                EXPIRED_CARD_NUMBER, EXPIRED_CARD_EXPIRATION, EXPIRED_CARD_PIN,
            new BigDecimal("75.00"), "Failure Comparison Test"
        );

        logger.info("🔄 1️⃣  REACTIVE APPROACH (CompletableFuture):");

        ReactivePaymentProcessor reactiveProcessor = new BasicReactivePaymentProcessor(balanceService,
                cardValidationService, expirationService, pinValidationService, merchantValidationService);
        long reactiveStart = System.currentTimeMillis();
        long reactiveTime;
        try {
            TransactionResult reactiveResult = reactiveProcessor.processTransaction(expiredCardRequest).get();
            reactiveTime = System.currentTimeMillis() - reactiveStart;
            logger.info("📊 Reactive completed in: {}ms", reactiveTime);
            printComparisonResult("REACTIVE", reactiveResult, reactiveTime);
        } catch (Exception e) {
            reactiveTime = System.currentTimeMillis() - reactiveStart;
            logger.info("📊 Reactive failed in: {}ms", reactiveTime);
            logger.info("❌ Reactive error: {}", e.getMessage());
        }


        logger.info("\n🚀 2️⃣  STRUCTURED CONCURRENCY APPROACH:");

        FailFastStructuredPaymentProcessor structuredProcessor = new FailFastStructuredPaymentProcessor(balanceService,
                cardValidationService, expirationService, pinValidationService, merchantValidationService);
        long structuredStart = System.currentTimeMillis();
        long structuredTime;
        try {
            TransactionResult structuredResult = structuredProcessor.processTransaction(expiredCardRequest);
            structuredTime = System.currentTimeMillis() - structuredStart;
            logger.info("📊 Structured completed in: {}ms", structuredTime);
            printComparisonResult("STRUCTURED", structuredResult, structuredTime);
        } catch (Exception e) {
            structuredTime = System.currentTimeMillis() - structuredStart;
            logger.info("📊 Structured failed in: {}ms", structuredTime);
            logger.info("❌ Structured error: {}", e.getMessage());
        }
        logger.info("\n📊 COMPARISON RESULTS:");
        logger.info("═════════════════════");
        logger.info("Reactive Processing Time:   {} ms\n", reactiveTime);
        logger.info("Structured Processing Time: {} ms\n", structuredTime);
        logger.info("Performance Difference:     {} ms\n", String.format("%+d", structuredTime - reactiveTime));

    }

    private void printComparisonResult(String approach, TransactionResult result, long timeMs) {
        logger.info("🎯 {} RESULT: {} in {}ms\n",
            approach,
            result.success() ? "SUCCESS" : "FAILED",
            timeMs);
        if (!result.success()) {
            logger.info("   💬 Failure reason: {}", result.message());
        }
    }
}
