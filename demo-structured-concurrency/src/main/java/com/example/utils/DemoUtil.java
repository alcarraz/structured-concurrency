package com.example.utils;

import com.example.model.TransactionResult;

/**
 * Utility class for common demo operations.
 * Used across all service classes to simulate realistic network delays,
 * format transaction results, and other demo-specific functionality.
 */
public class DemoUtil {

    /**
     * Simulates a network delay by sleeping the current thread.
     * This is used to make the demos more realistic and demonstrate
     * the timing differences between different concurrency approaches.
     *
     * @param millis The number of milliseconds to delay
     * @throws RuntimeException if the thread is interrupted
     */
    public static void simulateNetworkDelay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Service interrupted", e);
        }
    }

    /**
     * Prints a formatted transaction result to the console.
     * Used by all demo classes to provide consistent output formatting.
     *
     * @param result The transaction result to display
     */
    public static void printResult(TransactionResult result) {
        System.out.println("\n📋 TRANSACTION RESULT:");
        System.out.println("══════════════════════");

        if (result.success()) {
            System.out.println("✅ Status: SUCCESS");
            System.out.println("🆔 Transaction ID: " + result.transactionId());
            System.out.println("💰 Amount: $" + result.amount());
        } else {
            System.out.println("❌ Status: FAILED");
            System.out.println("💬 Reason: " + result.message());
        }

        System.out.println("⏱️  Processing Time: " + result.processingTimeMs() + "ms");
        System.out.println("📅 Processed At: " + result.processedAt());

        System.out.println("\n" + "═".repeat(50) + "\n");
    }
}