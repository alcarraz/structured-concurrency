package com.example.services;

import com.example.model.Card;
import com.example.model.TransactionRequest;
import com.example.model.ValidationResult;
import com.example.repository.CardRepository;
import com.example.utils.DemoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.math.BigDecimal.ZERO;

@ApplicationScoped
public class BalanceService implements ValidationService {
    private static final Logger logger = LogManager.getLogger(BalanceService.class);

    private final CardRepository cardRepository;

    // Pending transactions by card number (tracks which transactions have locked funds)
    private final ConcurrentHashMap<String, Set<TransactionRequest>> pendingTransactions = new ConcurrentHashMap<>();

    // Locks per card number for thread-safe operations
    private final ConcurrentHashMap<String, Lock> cardLocks = new ConcurrentHashMap<>();

    public BalanceService() {
        this(new CardRepository());
    }

    @Inject
    public BalanceService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
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
            // Get balance from CardRepository instead of local balances map
            BigDecimal cardBalance = cardRepository.findByCardNumber(cardNumber)
                    .map(Card::balance)
                    .orElse(ZERO);

            BigDecimal availableBalance = cardBalance.subtract(getLockedAmount(cardNumber));

            if (availableBalance.compareTo(amount) < 0) {
                return ValidationResult.failure("Balance Check: Insufficient funds (available: " + availableBalance + ")");
            }

            // Add transaction to pending set
            lockAmount(cardNumber, request);

            logger.info("🔒 Locked {} on card {}", amount, cardNumber.substring(cardNumber.length() - 4));
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
                logger.info("🔓 Unlocked {} on card {}", amount, cardNumber.substring(cardNumber.length() - 4));
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
//        DemoUtil.simulateNetworkDelay(500);

        String cardNumber = request.cardNumber();
        String merchant = request.merchant();
        BigDecimal amount = request.amount();

        Lock lock = getLock(cardNumber);
        lock.lock();
        try {
            // Get current balance from CardRepository
            BigDecimal currentBalance = cardRepository.findByCardNumber(cardNumber)
                    .map(Card::balance)
                    .orElse(ZERO);

            // Release the lock since we've consumed it
            releaseAmount(cardNumber, request);

            // Debit the actual balance and update in CardRepository (copy-on-write)
            BigDecimal newBalance = currentBalance.subtract(amount);
            cardRepository.findByCardNumber(cardNumber).ifPresent(card -> {
                Card updatedCard = new Card(
                    card.cardNumber(),
                    card.expirationDate(),
                    card.pin(),
                    newBalance,
                    card.description()
                );
                cardRepository.save(updatedCard);
            });

            // now we should put the money in the merchant account

            logger.info("💸 Transferring {} from card {} to {}", amount, cardNumber.substring(cardNumber.length() - 4), merchant);
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

    /**
     * Returns all card balances (for Web UI).
     */
    public Map<String, BigDecimal> getAllBalances() {
        return cardRepository.findAll().stream()
                .collect(HashMap::new,
                        (map, card) -> map.put(card.cardNumber(), card.balance()),
                        HashMap::putAll);
    }

    /**
     * Returns balance for specific card (for Web UI).
     */
    public BigDecimal getBalance(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .map(Card::balance)
                .orElse(ZERO);
    }

    /**
     * Sets balance for specific card (for Web UI demo purposes).
     */
    public void setBalance(String cardNumber, BigDecimal newBalance) {
        Lock lock = getLock(cardNumber);
        lock.lock();
        try {
            cardRepository.findByCardNumber(cardNumber).ifPresent(card -> {
                Card updatedCard = new Card(
                    card.cardNumber(),
                    card.expirationDate(),
                    card.pin(),
                    newBalance,
                    card.description()
                );
                cardRepository.save(updatedCard);
            });
        } finally {
            lock.unlock();
        }
    }
}
