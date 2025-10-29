package com.example;

import com.example.demos.CompareDemo;
import com.example.demos.CompareFailureDemo;
import com.example.demos.ReactiveDemo;
import com.example.demos.ScopedValuesDemo;
import com.example.demos.StructuredDemo;

public class DemoRunner {
    void main(String[] args) {
        if (args.length == 0) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           Structured Concurrency Java 25 Demo            ║");
            System.out.println("║              Financial Transaction Processing             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Usage: DemoRunner <demo-type>");
            System.out.println();
            System.out.println("Available demos:");
            System.out.println("  reactive              - CompletableFuture approach (complex, nested)");
            System.out.println("  reactive-exceptions   - Reactive with exceptions (still no fail-fast!)");
            System.out.println("  fixed-reactive-failfast- 'Fixed' reactive with manual fail-fast (still complex)");
            System.out.println("  structured            - Structured Concurrency (fail-fast by default)");
            System.out.println("  structured-normal     - Structured Concurrency (await all validations)");
            System.out.println("  scoped-values         - Scoped Values demo (context propagation)");
            System.out.println("  compare               - Side-by-side performance comparison");
            System.out.println("  compare-failure       - Early failure behavior comparison");
            System.out.println();
            System.out.println("Example scenarios:");
            System.out.println("  • Valid transaction:   Customer 12345, Card 4532-1234-5678-9012, PIN 1234");
            System.out.println("  • Expired card:        Customer 12345, Card 5555-4444-3333-2222, PIN 9876");
            System.out.println("  • Insufficient funds:  Customer 67890, Card 4532-1234-5678-9012, PIN 1234");
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
                default -> {
                    System.out.println("❌ Unknown demo type: " + demoType);
                    System.out.println("Run without arguments to see available options.");
                }
            }
        } catch (Exception e) {
            System.err.println("💥 Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
