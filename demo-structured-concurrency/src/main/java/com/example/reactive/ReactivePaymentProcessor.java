package com.example.reactive;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;

import java.util.concurrent.CompletableFuture;

/**
 * Common interface for all reactive payment processors.
 * This interface allows for polymorphic usage of different reactive
 * implementations while maintaining a consistent API.
 */
public interface ReactivePaymentProcessor {

    /**
     * Processes a transaction request asynchronously using reactive programming patterns.
     *
     * @param request The transaction request to process
     * @return A CompletableFuture that will complete with the transaction result
     */
    CompletableFuture<TransactionResult> processTransaction(TransactionRequest request);
}