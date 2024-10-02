package com.example;

import java.util.Objects;

import static com.example.Util.sleep;

public class Validations {

    public static final String INVALID_MERCHANT = "invalid-merchant";
    public static final double BALANCE = 1000;
    public static final String PIN_BLOCK = "1234";

    public static boolean checkMerchant(String merchantId) {
        try {
            sleep(1000);
            return !Objects.equals(INVALID_MERCHANT, merchantId); //throw new ValidationException("invalid merchant");
        } finally {
            System.out.println("merchant validation finished");
        }
    }
    
    public static boolean checkBalance(String cardNumber, double amount) {
        try {
            if (amount < 0) throw new IllegalArgumentException("negative amount");
            sleep(2000);
            return amount <= BALANCE; //throw new ValidationException("insufficient balance");
        } finally {
            System.out.println("balance validation finished");
        }
    }

    public static boolean checkPIN(String cardNumber, String pinBlock) {
        try {
            sleep(1000);
            //throw new ValidationException("invalid pin");
            sleep(3000);
            return Objects.equals(pinBlock, PIN_BLOCK);
        } finally {
            System.out.println("pin validation finished");
        }
    }
}
