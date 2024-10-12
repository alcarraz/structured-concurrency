package com.example.failfast;

import com.example.StopWatch;
import com.example.Transaction;
import com.example.ValidationException;

import java.util.concurrent.CompletableFuture;

import static com.example.Validations.*;
import static java.util.concurrent.CompletableFuture.allOf;

public class Reactive {
    public static void main() {
        StopWatch.measureTime(() ->  {
            Transaction tx = new Transaction("1234567890123456", "1234", 1001.0, "123456");
            CompletableFuture<Boolean> checkBalance = CompletableFuture.supplyAsync(() -> 
                    throwOnFailure( checkBalance(tx.cardNumber(), tx.amount()), "balance")
            );
            CompletableFuture<Boolean> checkMerchant = CompletableFuture.supplyAsync(() ->
                    throwOnFailure(checkMerchant(tx.merchantId()), "merchant")
            );
            CompletableFuture<Boolean> checkPIN = CompletableFuture.supplyAsync(() ->
                    throwOnFailure(checkPIN(tx.cardNumber(), tx.pinBlock()), "pin")
            );
            
            allOf(checkBalance, checkMerchant, checkPIN).join();
            System.out.println("All validations completed: true");
        });
    }
    
    static boolean throwOnFailure(boolean result, String what) {
        if (!result) throw new RuntimeException("result for %s check was false".formatted(what));
        return true;
    }
}
