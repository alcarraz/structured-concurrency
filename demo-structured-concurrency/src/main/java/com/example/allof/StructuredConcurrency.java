package com.example.allof;

import com.example.StopWatch;
import com.example.Transaction;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static com.example.Validations.checkBalance;
import static com.example.Validations.checkMerchant;
import static com.example.Validations.checkPIN;

public class StructuredConcurrency {
    public static void main() {
        StopWatch.measureTime(() -> {
            Transaction tx = new Transaction("1234567890123456", "1234", 100.0, "123456");
            try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
                Subtask<Boolean> checkBalance = scope.fork(() -> checkBalance(tx.cardNumber(), tx.amount()));
                Subtask<Boolean> checkMerchant = scope.fork(() -> checkMerchant(tx.merchantId()));
                Subtask<Boolean> checkPIN = scope.fork(() -> checkPIN(tx.cardNumber(), tx.pinBlock()));
                scope.join();
                boolean success = checkBalance.get() && checkMerchant.get() && checkPIN.get();
                System.out.println("All validations completed: " + success);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
