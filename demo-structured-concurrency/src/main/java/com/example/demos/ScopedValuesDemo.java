package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.scopedvalues.RequestContext;
import com.example.scopedvalues.ScopedPaymentProcessor;
import com.example.utils.DemoUtil;

import java.math.BigDecimal;

/**
 * Scoped Values Demo (STABLE in Java 25!)
 * <p>
 * This demo showcases context propagation using Scoped Values.
 * Demonstrates how to pass request context through concurrent tasks
 * without parameter drilling or ThreadLocal complexity.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class ScopedValuesDemo {
    public void main() throws Exception {
        System.out.println("🔗 Running SCOPED VALUES Demo");
        System.out.println("═════════════════════════════");

        ScopedPaymentProcessor processor = new ScopedPaymentProcessor();
        RequestContext context = RequestContext.create(
            "sess_12345", "Mozilla/5.0", "192.168.1.100"
        );

        // Valid transaction with context
        TransactionRequest validRequest = new TransactionRequest(
            "11111", "4111-1111-1111-1111", "2512", "5555",  // December 2025 (valid)
            new BigDecimal("250.00"), "Restaurant"
        );

        TransactionResult result = processor.processTransaction(validRequest, context);
        DemoUtil.printResult(result);
    }
}
