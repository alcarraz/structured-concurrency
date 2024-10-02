package com.example;

public record Transaction(String cardNumber, String pinBlock, double amount, String merchantId) {
}
