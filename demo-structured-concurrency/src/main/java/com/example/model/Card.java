package com.example.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Card {
    private String cardNumber;
    private String expirationDate; // YYMM format
    private String pin;
    private BigDecimal balance;
    private String description;

    public Card() {
    }

    public Card(String cardNumber, String expirationDate, String pin, BigDecimal balance, String description) {
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.pin = pin;
        this.balance = balance;
        this.description = description;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(cardNumber, card.cardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber);
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + cardNumber + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", pin='" + pin + '\'' +
                ", balance=" + balance +
                ", description='" + description + '\'' +
                '}';
    }
}
