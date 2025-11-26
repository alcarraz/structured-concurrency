package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

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
        logger.info("💥 Running EARLY FAILURE BEHAVIOR COMPARISON Demo");
        logger.info("════════════════════════════════════════════════");
        logger.info("⚠️  Using EXPIRED CARD scenario to demonstrate early failure handling\n");

        // Use expired card request to trigger early failure
        TransactionRequest expiredCardRequest = new TransactionRequest(
                "5555-4444-3333-2222", "2312", "9876",  // December 2023 (expired)
            new BigDecimal("75.00"), "Failure Comparison Test"
        );

        logger.info("🔄 1️⃣  REACTIVE APPROACH (CompletableFuture):");

        ReactivePaymentProcessor reactiveProcessor = new BasicReactivePaymentProcessor();
        long reactiveStart = System.currentTimeMillis();
        long reactiveTime;
        try {
            TransactionResult reactiveResult = reactiveProcessor.processTransaction(expiredCardRequest).get();
            reactiveTime = System.currentTimeMillis() - reactiveStart;
            logger.info("📊 Reactive completed in: " + reactiveTime + "ms");
            printComparisonResult("REACTIVE", reactiveResult, reactiveTime);
        } catch (Exception e) {
            reactiveTime = System.currentTimeMillis() - reactiveStart;
            logger.info("📊 Reactive failed in: " + reactiveTime + "ms");
            logger.info("❌ Reactive error: " + e.getMessage());
        }


        logger.info("\n🚀 2️⃣  STRUCTURED CONCURRENCY APPROACH:");

        FailFastStructuredPaymentProcessor structuredProcessor = new FailFastStructuredPaymentProcessor();
        long structuredStart = System.currentTimeMillis();
        long structuredTime;
        try {
            TransactionResult structuredResult = structuredProcessor.processTransaction(expiredCardRequest);
            structuredTime = System.currentTimeMillis() - structuredStart;
            logger.info("📊 Structured completed in: " + structuredTime + "ms");
            printComparisonResult("STRUCTURED", structuredResult, structuredTime);
        } catch (Exception e) {
            structuredTime = System.currentTimeMillis() - structuredStart;
            logger.info("📊 Structured failed in: " + structuredTime + "ms");
            logger.info("❌ Structured error: " + e.getMessage());
        }
        logger.info("\n📊 COMPARISON RESULTS:");
        logger.info("═════════════════════");
        logger.info(String.format("Reactive Processing Time:   %d ms%n", reactiveTime));
        logger.info(String.format("Structured Processing Time: %d ms%n", structuredTime));
        logger.info(String.format("Performance Difference:     %+d ms%n", structuredTime - reactiveTime));

    }

    private void printComparisonResult(String approach, TransactionResult result, long timeMs) {
        logger.info(String.format("🎯 %s RESULT: %s in %dms%n",
            approach,
            result.success() ? "SUCCESS" : "FAILED",
            timeMs));
        if (!result.success()) {
            logger.info("   💬 Failure reason: " + result.message());
        }
    }
}
