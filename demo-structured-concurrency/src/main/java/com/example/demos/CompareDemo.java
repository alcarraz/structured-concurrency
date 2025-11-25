package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.StructuredProcessor;

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
    public void main() throws Exception {
        System.out.println("⚖️  Running PERFORMANCE COMPARISON Demo");
        System.out.println("════════════════════════════════════════");

        TransactionRequest request = new TransactionRequest(
                "4532-1234-5678-9012", "2512", "1234",  // December 2025 (valid)
            new BigDecimal("100.00"), "Comparison Test"
        );

        System.out.println("\n1️⃣  REACTIVE APPROACH:");
        System.out.println("─────────────────────");
        ReactivePaymentProcessor reactiveProcessor = new BasicReactivePaymentProcessor();
        long reactiveStart = System.currentTimeMillis();
        reactiveProcessor.processTransaction(request).get();
        long reactiveTime = System.currentTimeMillis() - reactiveStart;

        System.out.println("\n2️⃣  STRUCTURED CONCURRENCY APPROACH:");
        System.out.println("─────────────────────────────────────");
        StructuredProcessor structuredProcessor = new StructuredPaymentProcessor();
        long structuredStart = System.currentTimeMillis();
        structuredProcessor.processTransaction(request);
        long structuredTime = System.currentTimeMillis() - structuredStart;

        System.out.println("\n📊 COMPARISON RESULTS:");
        System.out.println("═════════════════════");
        System.out.printf("Reactive Processing Time:   %d ms%n", reactiveTime);
        System.out.printf("Structured Processing Time: %d ms%n", structuredTime);
        System.out.printf("Performance Difference:     %+d ms%n", structuredTime - reactiveTime);
    }
}
