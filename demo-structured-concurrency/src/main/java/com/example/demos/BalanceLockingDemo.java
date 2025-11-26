package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

/**
 * Balance Locking/Unlocking Demo
 * <p>
 * Demonstrates how the balance service locks funds during validation
 * and automatically unlocks them if the transaction fails.
 */
public class BalanceLockingDemo {
    private static final Logger logger = LogManager.getLogger(BalanceLockingDemo.class);

    public void main() {
        logger.info("🔐 Running BALANCE LOCKING/UNLOCKING Demo");
        logger.info("════════════════════════════════════════");
        logger.info("");

        // Test 1: Successful transaction - lock then transfer
        logger.info("📝 Test 1: SUCCESSFUL TRANSACTION");
        logger.info("   Card: 1234-5678-9012-3456 (Balance: 5000)");
        logger.info("   Amount: 100");
        logger.info("   Merchant: TestMerchant");
        logger.info("");

        TransactionRequest successRequest = new TransactionRequest(
            "1234-5678-9012-3456", "2512", "1234",
            new BigDecimal("100.00"), "TestMerchant"
        );

        StructuredPaymentProcessor processor = new StructuredPaymentProcessor();
        try {
            TransactionResult result = processor.processTransaction(successRequest);
            logger.info("");
            logger.info("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                logger.info("   Reason: " + result.message());
            }
        } catch (Exception e) {
            logger.info("❌ Error: " + e.getMessage());
        }

        logger.info("");
        logger.info("═══════════════════════════════════════");
        logger.info("");

        // Test 2: Failed transaction - lock then unlock
        logger.info("📝 Test 2: FAILED TRANSACTION (Blocked Merchant)");
        logger.info("   Card: 1234-5678-9012-3456 (Balance: ~4900 after test 1)");
        logger.info("   Amount: 200");
        logger.info("   Merchant: BLOCKED_Merchant (will fail validation)");
        logger.info("");

        TransactionRequest failedRequest = new TransactionRequest(
            "1234-5678-9012-3456", "2512", "1234",
            new BigDecimal("200.00"), "BLOCKED_Merchant"
        );

        StructuredPaymentProcessor processor2 = new StructuredPaymentProcessor();
        try {
            TransactionResult result = processor2.processTransaction(failedRequest);
            logger.info("");
            logger.info("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                logger.info("   Reason: " + result.message());
            }
        } catch (Exception e) {
            logger.info("❌ Error: " + e.getMessage());
        }

        logger.info("");
        logger.info("═══════════════════════════════════════");
        logger.info("");

        // Test 3: Fail-fast with balance lock/unlock
        logger.info("📝 Test 3: FAIL-FAST (PIN failure)");
        logger.info("   Card: 9876-5432-1098-7654 (Balance: 500)");
        logger.info("   Amount: 50");
        logger.info("   PIN: 0000 (invalid - will fail)");
        logger.info("");

        TransactionRequest failFastRequest = new TransactionRequest(
            "9876-5432-1098-7654", "2512", "0000",
            new BigDecimal("50.00"), "TestMerchant"
        );

        FailFastStructuredPaymentProcessor failFastProcessor = new FailFastStructuredPaymentProcessor();
        try {
            TransactionResult result = failFastProcessor.processTransaction(failFastRequest);
            logger.info("");
            logger.info("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                logger.info("   Reason: " + result.message());
            }
        } catch (Exception e) {
            logger.info("❌ Error: " + e.getMessage());
        }

        logger.info("");
        logger.info("🎯 DEMO COMPLETE");
        logger.info("");
        logger.info("Key observations:");
        logger.info("• Test 1: Balance locked (🔒) → Transfer successful (💸)");
        logger.info("• Test 2: Balance locked (🔒) → Merchant failed → Balance unlocked (🔓)");
        logger.info("• Test 3: Balance locked (🔒) → PIN failed → Balance unlocked (🔓) + tasks cancelled");
    }
}
