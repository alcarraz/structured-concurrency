package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.reactive.ReactiveWithExceptionsPaymentProcessor;
import com.example.reactive.FixedReactiveFailFastPaymentProcessor;
import com.example.utils.DemoUtil;

import java.math.BigDecimal;

/**
 * Unified Reactive Programming Demo
 * <p>
 * This demo showcases different reactive approaches to concurrent transaction processing.
 * Supports different processor implementations based on command-line argument.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class ReactiveDemo {

    enum Type {
        BASIC("Basic Reactive (CompletableFuture)"),
        EXCEPTIONS("Reactive with Exceptions"),
        FAIL_FAST("Fixed Reactive Fail-Fast");

        final String description;
        Type(String description) {
            this.description = description;
        }
    }

    public void main(String... args) {
        Type processorType = getProcessorType(args.length > 0 ? args[0] : "basic");

        ReactivePaymentProcessor processor = createProcessor(processorType);

        System.out.println("🔄 Running REACTIVE Demo - " + processorType.description);
        System.out.println("═══════════════════════════════════════════");
        
        //Simulate failure when trying to demo fail fast behavior.
        String expDate = (processorType == Type.BASIC) ? "2512" : "2312";
        
        // Valid transaction
        TransactionRequest validRequest = new TransactionRequest(
                "4532-1234-5678-9012", expDate, "1234",  // December 2025 (valid)
            new BigDecimal("100.00"), "Coffee Shop"
        );

        try {
            TransactionResult result = processor.processTransaction(validRequest).get();
            DemoUtil.printResult(result);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private ReactivePaymentProcessor createProcessor(Type type) {
        return switch (type) {
            case BASIC -> new BasicReactivePaymentProcessor();
            case EXCEPTIONS -> new ReactiveWithExceptionsPaymentProcessor();
            case FAIL_FAST -> new FixedReactiveFailFastPaymentProcessor();
        };
    }

    private Type getProcessorType(String type) {
        return switch (type.toLowerCase()) {
            case "exceptions" -> Type.EXCEPTIONS;
            case "fail-fast" -> Type.FAIL_FAST;
            default -> Type.BASIC;
        };
    }
}
