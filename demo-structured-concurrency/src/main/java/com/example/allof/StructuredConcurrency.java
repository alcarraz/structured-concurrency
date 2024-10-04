package com.example.allof;

import com.example.Transaction;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static com.example.Validations.checkBalance;
import static com.example.Validations.checkMerchant;
import static com.example.Validations.checkPIN;

public class StructuredConcurrency {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Transaction tx = new Transaction("1234567890123456", "1234", 100.0, "123456");
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Subtask<Boolean> checkBalance = scope.fork(() -> checkBalance(tx.cardNumber(), tx.amount()));
            Subtask<Boolean> checkMerchant = scope.fork(() -> checkMerchant(tx.merchantId()));
            Subtask<Boolean> checkPIN = scope.fork(() -> checkPIN(tx.cardNumber(), tx.pinBlock()));
            scope.join()
                    .throwIfFailed(); // If this is missing, an IllegalStateException is thrown if a subtask fails
            boolean success = checkBalance.get() && checkMerchant.get() && checkPIN.get();
            System.out.println("All validations completed: " + success );
        } finally {
            System.out.println("Finishing");
        }
    }
}
