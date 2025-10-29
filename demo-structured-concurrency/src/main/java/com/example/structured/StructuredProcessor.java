package com.example.structured;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;

/**
 * Common interface for all structured concurrency payment processors.
 * This interface allows for polymorphic usage of different structured concurrency
 * implementations while maintaining a consistent API.
 */
public interface StructuredProcessor {

    /**
     * Processes a transaction request using structured concurrency patterns.
     *
     * @param request The transaction request to process
     * @return A TransactionResult with the processing outcome
     * @throws InterruptedException if the thread is interrupted during processing
     */
    TransactionResult processTransaction(TransactionRequest request) throws InterruptedException;
}