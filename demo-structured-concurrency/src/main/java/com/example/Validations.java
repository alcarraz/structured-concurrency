package com.example;

import java.util.Objects;

import static com.example.Util.sleep;

public class Validations {

    public static final String INVALID_MERCHANT = "invalid-merchant";
    public static final double BALANCE = 1000;
    public static final String VALID_PIN_BLOCK = "1234";

    public static boolean checkMerchant(String merchantId) {
        StopWatch stopWatch = StopWatch.start("merchant validation");
        try {
            sleep(1000);
            return !Objects.equals(INVALID_MERCHANT, merchantId); //throw new ValidationException("invalid merchant");
        } finally {
            System.out.println(stopWatch);
        }
    }
    
    public static boolean checkBalance(String cardNumber, double amount) {
        StopWatch stopWatch = StopWatch.start("merchant validation");
        try {
            if (amount < 0) throw new IllegalArgumentException("negative amount");
            sleep(2000);
            return amount <= BALANCE; 
        } finally {
            System.out.println(stopWatch);
        }
    }

    public static boolean checkPIN(String cardNumber, String pinBlock) {
        StopWatch stopWatch = StopWatch.start("merchant validation");
        try {
            sleep(3000);
            return Objects.equals(pinBlock, VALID_PIN_BLOCK);
        } finally {
            System.out.println(stopWatch);
        }
    }
}
