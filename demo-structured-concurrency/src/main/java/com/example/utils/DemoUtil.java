package com.example.utils;

import com.example.model.TransactionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for common demo operations.
 * Used across all service classes to simulate realistic network delays,
 * format transaction results, and other demo-specific functionality.
 */
public class DemoUtil {
    private static final Logger logger = LogManager.getLogger(DemoUtil.class);

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
        logger.info("\n📋 TRANSACTION RESULT:");
        logger.info("══════════════════════");

        if (result.success()) {
            logger.info("✅ Status: SUCCESS");
            logger.info("🆔 Transaction ID: " + result.transactionId());
            logger.info("💰 Amount: $" + result.amount());
        } else {
            logger.info("❌ Status: FAILED");
            logger.info("💬 Reason: " + result.message());
        }

        logger.info("⏱️  Processing Time: " + result.processingTimeMs() + "ms");
        logger.info("📅 Processed At: " + result.processedAt());

        logger.info("\n" + "═".repeat(50) + "\n");
    }
}