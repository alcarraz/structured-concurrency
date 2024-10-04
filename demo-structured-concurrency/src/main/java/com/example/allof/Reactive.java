package com.example.allof;

import com.example.Transaction;

import java.util.concurrent.CompletableFuture;

import static com.example.Validations.checkBalance;
import static com.example.Validations.checkMerchant;
import static com.example.Validations.checkPIN;
import static java.util.concurrent.CompletableFuture.allOf;

public class Reactive {
    public static void main(String[] args) {

        Transaction tx = new Transaction("1234567890123456", "1234", -100.0, "123456");
        CompletableFuture<Boolean> checkBalance = CompletableFuture.supplyAsync(() ->
                checkBalance(tx.cardNumber(), tx.amount()));
        CompletableFuture<Boolean> checkMerchant = CompletableFuture.supplyAsync(() ->
                checkMerchant(tx.merchantId()));
        CompletableFuture<Boolean> checkPIN = CompletableFuture.supplyAsync(() ->
                checkPIN(tx.cardNumber(), tx.pinBlock()));
        boolean success = allOf(checkBalance, checkMerchant, checkPIN).thenApply(_ -> 
                    checkBalance.join() && checkMerchant.join() && checkPIN.join()
        ).exceptionally(e -> {
            throw new RuntimeException(e); //can only throw unchecked exceptions
        }).join();
        System.out.println("All validations completed: " + success);
    }
}
