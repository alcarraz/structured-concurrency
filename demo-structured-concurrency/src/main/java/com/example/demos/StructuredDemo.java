package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.StructuredProcessor;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.utils.DemoUtil;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unified Structured Concurrency Demo
 * <p>
 * This demo showcases different structured concurrency approaches to concurrent transaction processing.
 * Supports different processor implementations based on command-line argument.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class StructuredDemo {
    private static final Logger logger = LogManager.getLogger(StructuredDemo.class);

    enum Type {
        NORMAL("Normal Structured Concurrency (Await All)"), FAIL_FAST("Fail-Fast Structured Concurrency");
        final String description;
        Type(String description) {
            this.description = description;
        }
    }
    public void main(String... args) throws InterruptedException {
        Type processorType = (args.length > 0 && args[0].equalsIgnoreCase("fail-fast")) ? Type.FAIL_FAST : Type.NORMAL;

        StructuredProcessor processor = createProcessor(processorType);

        logger.info("🚀 Running STRUCTURED CONCURRENCY Demo - {}", processorType.description);
        logger.info("═════════════════════════════════════════════════════════");

        // Use expired card for fail-fast demo, valid card for normal demo
        TransactionRequest request = createTestRequest(processorType);

        TransactionResult result = processor.processTransaction(request);
        DemoUtil.printResult(result);
    }

    private StructuredProcessor createProcessor(Type type) {
        return switch (type) {
            case FAIL_FAST -> new FailFastStructuredPaymentProcessor();
            case NORMAL -> new StructuredPaymentProcessor();
        };
    }

    private TransactionRequest createTestRequest(Type type) {
        return switch (type) {
            case NORMAL -> new TransactionRequest(
                    "4532-1234-5678-9012", "2512", "1234",  // December 2025 (valid)
                new BigDecimal("100.00"), "Coffee Shop"
            );
            case FAIL_FAST -> new TransactionRequest(  // fail-fast case
                    "5555-4444-3333-2222", "2312", "9876",  // December 2023 (expired)
                new BigDecimal("75.00"), "Online Store"
            );
        };
    }
}
