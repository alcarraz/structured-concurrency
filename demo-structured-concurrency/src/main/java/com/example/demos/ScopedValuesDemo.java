package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.scopedvalues.ScopedPaymentProcessor;
import com.example.utils.DemoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

import static com.example.fixtures.DemoCards.*;

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
    private static final Logger logger = LogManager.getLogger(ScopedValuesDemo.class);

    public void main() throws Exception {
        logger.info("🔗 Running SCOPED VALUES Demo");
        logger.info("═════════════════════════════");

        ScopedPaymentProcessor processor = new ScopedPaymentProcessor();

        // Valid transaction - request data will be accessible via ScopedValue
        TransactionRequest validRequest = new TransactionRequest(
                SCOPED_CARD_NUMBER, SCOPED_CARD_EXPIRATION, SCOPED_CARD_PIN,
            new BigDecimal("250.00"), "Restaurant"
        );

        TransactionResult result = processor.processTransaction(validRequest);
        DemoUtil.printResult(result);
    }
}
