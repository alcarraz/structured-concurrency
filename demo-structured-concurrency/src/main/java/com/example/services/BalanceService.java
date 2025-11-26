package com.example.services;

import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.utils.DemoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.math.BigDecimal.ZERO;

public class BalanceService implements ValidationService {
    private static final Logger logger = LogManager.getLogger(BalanceService.class);

    // Account balances by card number
    private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

    // Pending transactions by card number (tracks which transactions have locked funds)
    private final ConcurrentHashMap<String, Set<TransactionRequest>> pendingTransactions = new ConcurrentHashMap<>();

    // Locks per card number for thread-safe operations
    private final ConcurrentHashMap<String, Lock> cardLocks = new ConcurrentHashMap<>();

    public BalanceService() {
        // Initialize with demo account data
        balances.put("1234-5678-9012-3456", new BigDecimal("5000"));
        balances.put("9876-5432-1098-7654", new BigDecimal("500"));
        balances.put("1111-2222-3333-4444", new BigDecimal("100"));
    }

    private Lock getLock(String cardNumber) {
        return cardLocks.computeIfAbsent(cardNumber, _ -> new ReentrantLock());
    }

    private Set<TransactionRequest> getPendingTransactions(String cardNumber) {
        return pendingTransactions.computeIfAbsent(cardNumber, _ -> new HashSet<>());
    }

    @Override
    public ValidationResult validate(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(500);

        String cardNumber = request.cardNumber();
        BigDecimal amount = request.amount();

        Lock lock = getLock(cardNumber);
        lock.lock();
        try {
            BigDecimal availableBalance = getAvailableBalance(cardNumber);

            if (availableBalance.compareTo(amount) < 0) {
                return ValidationResult.failure("Balance Check: Insufficient funds (available: " + availableBalance + ")");
            }

            // Add transaction to pending set
            lockAmount(cardNumber, request);

            logger.info("🔒 Locked " + amount + " on card " + cardNumber.substring(cardNumber.length() - 4));
            return ValidationResult.success("Balance Check: Validation successful (locked " + amount + ")");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases (unlocks) the amount that was locked during validation.
     * Called when the transaction fails.
     * Idempotent - safe to call even if transaction was never locked.
     */
    public void releaseAmount(TransactionRequest request) {
        String cardNumber = request.cardNumber();
        BigDecimal amount = request.amount();

        Lock lock = getLock(cardNumber);
        lock.lock();
        try {
            if (releaseAmount(cardNumber, request)) {
                logger.info("🔓 Unlocked " + amount + " on card " + cardNumber.substring(cardNumber.length() - 4));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Transfers the amount from card to merchant.
     * Consumes the locked amount and debits the balance.
     */
    public void transfer(TransactionRequest request) {
        DemoUtil.simulateNetworkDelay(500);

        String cardNumber = request.cardNumber();
        String merchant = request.merchant();
        BigDecimal amount = request.amount();

        Lock lock = getLock(cardNumber);
        lock.lock();
        try {
            BigDecimal currentBalance = balances.getOrDefault(cardNumber, ZERO);

            // Release the lock since we've consumed it
            releaseAmount(cardNumber, request);

            // Debit the actual balance
            balances.put(cardNumber, currentBalance.subtract(amount));

            // now we should put the money in the merchant account

            logger.info("💸 Transferring " + amount + " from card " + cardNumber.substring(cardNumber.length() - 4) + " to " + merchant);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Private helper - removes transaction from pending set.
     * Returns true if transaction was actually removed (was pending).
     */
    private boolean releaseAmount(String cardNumber, TransactionRequest request) {
        return getPendingTransactions(cardNumber).remove(request);
    }

    private BigDecimal getAvailableBalance(String cardNumber) {
        return balances.computeIfAbsent(cardNumber, _ -> ZERO).subtract(getLockedAmount(cardNumber));
    }

    /**
     * Calculates total locked amount by summing all pending transaction amounts.
     */
    private BigDecimal getLockedAmount(String cardNumber) {
        return getPendingTransactions(cardNumber).stream()
                     .map(TransactionRequest::amount)
                     .reduce(ZERO, BigDecimal::add);
    }

    /**
     * Adds transaction to the pending set for this card.
     */
    private void lockAmount(String cardNumber, TransactionRequest request) {
        getPendingTransactions(cardNumber).add(request);
    }
}
