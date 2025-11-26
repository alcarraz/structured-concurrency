package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.StructuredProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

/**
 * Performance Comparison Demo
 * <p>
 * This demo compares the performance of Reactive vs Structured Concurrency approaches
 * using identical transaction scenarios. Shows that structured concurrency provides
 * the same performance with much cleaner, more maintainable code.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class CompareDemo {
    private static final Logger logger = LogManager.getLogger(CompareDemo.class);

    public void main() throws Exception {
        logger.info("⚖️  Running PERFORMANCE COMPARISON Demo");
        logger.info("════════════════════════════════════════");

        TransactionRequest request = new TransactionRequest(
                "4532-1234-5678-9012", "2512", "1234",  // December 2025 (valid)
            new BigDecimal("100.00"), "Comparison Test"
        );

        logger.info("\n1️⃣  REACTIVE APPROACH:");
        logger.info("─────────────────────");
        ReactivePaymentProcessor reactiveProcessor = new BasicReactivePaymentProcessor();
        long reactiveStart = System.currentTimeMillis();
        reactiveProcessor.processTransaction(request).get();
        long reactiveTime = System.currentTimeMillis() - reactiveStart;

        logger.info("\n2️⃣  STRUCTURED CONCURRENCY APPROACH:");
        logger.info("─────────────────────────────────────");
        StructuredProcessor structuredProcessor = new StructuredPaymentProcessor();
        long structuredStart = System.currentTimeMillis();
        structuredProcessor.processTransaction(request);
        long structuredTime = System.currentTimeMillis() - structuredStart;

        logger.info("\n📊 COMPARISON RESULTS:");
        logger.info("═════════════════════");
        logger.info(String.format("Reactive Processing Time:   %d ms%n", reactiveTime));
        logger.info(String.format("Structured Processing Time: %d ms%n", structuredTime));
        logger.info(String.format("Performance Difference:     %+d ms%n", structuredTime - reactiveTime));
    }
}
