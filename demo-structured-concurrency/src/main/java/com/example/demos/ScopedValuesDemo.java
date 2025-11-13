package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.scopedvalues.ScopedPaymentProcessor;
import com.example.utils.DemoUtil;

import java.math.BigDecimal;

/**
 * Scoped Values Demo (STABLE in Java 25!)
 * <p>
 * This demo showcases context propagation using Scoped Values.
 * Demonstrates how to pass the transaction request through concurrent tasks
 * without parameter drilling or ThreadLocal complexity.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class ScopedValuesDemo {
    public void main() throws Exception {
        System.out.println("🔗 Running SCOPED VALUES Demo");
        System.out.println("═════════════════════════════");

        ScopedPaymentProcessor processor = new ScopedPaymentProcessor();

        // Valid transaction - request data will be accessible via ScopedValue
        TransactionRequest validRequest = new TransactionRequest(
                "4111-1111-1111-1111", "2512", "5555",  // December 2025 (valid)
            new BigDecimal("250.00"), "Restaurant"
        );

        TransactionResult result = processor.processTransaction(validRequest);
        DemoUtil.printResult(result);
    }
}
