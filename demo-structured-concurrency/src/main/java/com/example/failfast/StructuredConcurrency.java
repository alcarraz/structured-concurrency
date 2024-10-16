package com.example.failfast;

import com.example.StopWatch;
import com.example.Transaction;
import com.example.ValidationException;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static com.example.Validations.*;

public class StructuredConcurrency {
    public static void main() {
        StopWatch.measureTime(() -> {
            Transaction tx = new Transaction("1234567890123456", "1234", 1001.0, "123456");
            try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
                Subtask<Boolean> checkBalance 
                        = scope.fork(() -> throwOnFailure(checkBalance(tx.cardNumber(), tx.amount()), "balance"));
                Subtask<Boolean> checkMerchant 
                        = scope.fork((() -> throwOnFailure(checkMerchant(tx.merchantId()), "merchant")));
                Subtask<Boolean> checkPIN 
                        = scope.fork(() -> throwOnFailure(checkPIN(tx.cardNumber(), tx.pinBlock()), "pin")); 
                scope.join();
                System.out.printf("All validations completed: %s%n",  
                        scope.exception().isEmpty() && checkMerchant.get() && checkBalance.get() && checkPIN.get()
                );
                scope.exception().filter(e -> !(e instanceof ValidationException)).ifPresent(Throwable::printStackTrace);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    static boolean throwOnFailure(boolean result, String what) throws ValidationException {
        if (!result) throw new ValidationException(what);
        return true;
    }
    
}
