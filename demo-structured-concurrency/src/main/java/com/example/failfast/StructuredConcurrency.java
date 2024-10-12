package com.example.failfast;

import com.example.StopWatch;
import com.example.Transaction;
import com.example.ValidationException;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static com.example.Validations.*;

public class StructuredConcurrency {
    public static void main() {
        StopWatch.measureTime(() -> {
            Transaction tx = new Transaction("1234567890123456", "1234", 1001.0, "123456");
            try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
                scope.fork(() -> shutdownOnFailure(checkBalance(tx.cardNumber(), tx.amount()), scope));
                scope.fork(() -> shutdownOnFailure(checkMerchant(tx.merchantId()), scope));
                scope.fork(() -> shutdownOnFailure(checkPIN(tx.cardNumber(), tx.pinBlock()), scope));
                scope.join();
                System.out.println("All validations completed: ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    static boolean shutdownOnFailure(boolean result, StructuredTaskScope.ShutdownOnFailure scope) {
        if (!result) scope.shutdown();
        return result;
    }
}
