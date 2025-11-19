package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;

import java.math.BigDecimal;

/**
 * Balance Locking/Unlocking Demo
 * <p>
 * Demonstrates how the balance service locks funds during validation
 * and automatically unlocks them if the transaction fails.
 */
public class BalanceLockingDemo {
    public void main() {
        System.out.println("🔐 Running BALANCE LOCKING/UNLOCKING Demo");
        System.out.println("════════════════════════════════════════");
        System.out.println();

        // Test 1: Successful transaction - lock then transfer
        System.out.println("📝 Test 1: SUCCESSFUL TRANSACTION");
        System.out.println("   Card: 1234-5678-9012-3456 (Balance: 5000)");
        System.out.println("   Amount: 100");
        System.out.println("   Merchant: TestMerchant");
        System.out.println();

        TransactionRequest successRequest = new TransactionRequest(
            "1234-5678-9012-3456", "2512", "1234",
            new BigDecimal("100.00"), "TestMerchant"
        );

        StructuredPaymentProcessor processor = new StructuredPaymentProcessor();
        try {
            TransactionResult result = processor.processTransaction(successRequest);
            System.out.println();
            System.out.println("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                System.out.println("   Reason: " + result.message());
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println();

        // Test 2: Failed transaction - lock then unlock
        System.out.println("📝 Test 2: FAILED TRANSACTION (Blocked Merchant)");
        System.out.println("   Card: 1234-5678-9012-3456 (Balance: ~4900 after test 1)");
        System.out.println("   Amount: 200");
        System.out.println("   Merchant: BLOCKED_Merchant (will fail validation)");
        System.out.println();

        TransactionRequest failedRequest = new TransactionRequest(
            "1234-5678-9012-3456", "2512", "1234",
            new BigDecimal("200.00"), "BLOCKED_Merchant"
        );

        StructuredPaymentProcessor processor2 = new StructuredPaymentProcessor();
        try {
            TransactionResult result = processor2.processTransaction(failedRequest);
            System.out.println();
            System.out.println("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                System.out.println("   Reason: " + result.message());
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println();

        // Test 3: Fail-fast with balance lock/unlock
        System.out.println("📝 Test 3: FAIL-FAST (PIN failure)");
        System.out.println("   Card: 9876-5432-1098-7654 (Balance: 500)");
        System.out.println("   Amount: 50");
        System.out.println("   PIN: 0000 (invalid - will fail)");
        System.out.println();

        TransactionRequest failFastRequest = new TransactionRequest(
            "9876-5432-1098-7654", "2512", "0000",
            new BigDecimal("50.00"), "TestMerchant"
        );

        FailFastStructuredPaymentProcessor failFastProcessor = new FailFastStructuredPaymentProcessor();
        try {
            TransactionResult result = failFastProcessor.processTransaction(failFastRequest);
            System.out.println();
            System.out.println("✅ Result: " + (result.success() ? "SUCCESS" : "FAILED"));
            if (!result.success()) {
                System.out.println("   Reason: " + result.message());
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("🎯 DEMO COMPLETE");
        System.out.println();
        System.out.println("Key observations:");
        System.out.println("• Test 1: Balance locked (🔒) → Transfer successful (💸)");
        System.out.println("• Test 2: Balance locked (🔒) → Merchant failed → Balance unlocked (🔓)");
        System.out.println("• Test 3: Balance locked (🔒) → PIN failed → Balance unlocked (🔓) + tasks cancelled");
    }
}
