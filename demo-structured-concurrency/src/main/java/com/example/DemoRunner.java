package com.example;

import com.example.demos.BalanceLockingDemo;
import com.example.demos.CompareDemo;
import com.example.demos.CompareFailureDemo;
import com.example.demos.ReactiveDemo;
import com.example.demos.ScopedValuesDemo;
import com.example.demos.StructuredDemo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemoRunner {
    private static final Logger logger = LogManager.getLogger(DemoRunner.class);

    void main(String[] args) {
        if (args.length == 0) {
            logger.info("╔══════════════════════════════════════════════════════════╗");
            logger.info("║           Structured Concurrency Java 25 Demo            ║");
            logger.info("║             Financial Transaction Processing             ║");
            logger.info("╚══════════════════════════════════════════════════════════╝");
            logger.info("");
            logger.info("Usage: DemoRunner <demo-type>");
            logger.info("");
            logger.info("Available demos:");
            logger.info("  reactive              - CompletableFuture approach (complex, nested)");
            logger.info("  reactive-exceptions   - Reactive with exceptions (still no fail-fast!)");
            logger.info("  fixed-reactive-failfast- 'Fixed' reactive with manual fail-fast (still complex)");
            logger.info("  structured            - Structured Concurrency (fail-fast by default)");
            logger.info("  structured-normal     - Structured Concurrency (await all validations)");
            logger.info("  scoped-values         - Scoped Values demo (context propagation)");
            logger.info("  compare               - Side-by-side performance comparison");
            logger.info("  compare-failure       - Early failure behavior comparison");
            logger.info("  balance-locking       - Balance lock/unlock behavior demo");
            logger.info("");
            logger.info("Example scenarios:");
            logger.info("  • Valid transaction:   Customer 12345, Card 4532-1234-5678-9012, PIN 1234");
            logger.info("  • Expired card:        Customer 12345, Card 5555-4444-3333-2222, PIN 9876");
            logger.info("  • Insufficient funds:  Customer 67890, Card 4532-1234-5678-9012, PIN 1234");
            return;
        }

        String demoType = args[0];

        try {
            switch (demoType) {
                case "reactive" -> new ReactiveDemo().main("basic");
                case "reactive-exceptions" -> new ReactiveDemo().main("exceptions");
                case "fixed-reactive-failfast" -> new ReactiveDemo().main("fail-fast");
                case "structured" -> new StructuredDemo().main("fail-fast");
                case "structured-normal" -> new StructuredDemo().main("normal");
                case "scoped-values" -> new ScopedValuesDemo().main();
                case "compare" -> new CompareDemo().main();
                case "compare-failure" -> new CompareFailureDemo().main();
                case "balance-locking" -> new BalanceLockingDemo().main();
                default -> {
                    logger.info("❌ Unknown demo type: " + demoType);
                    logger.info("Run without arguments to see available options.");
                }
            }
        } catch (Exception e) {
            logger.error("💥 Demo failed: " + e.getMessage(), e);
        }
    }
}
