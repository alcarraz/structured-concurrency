package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;

@Path("/api/compare")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComparisonResource {
    private static final Logger logger = LogManager.getLogger(ComparisonResource.class);

    @Inject
    BasicReactivePaymentProcessor reactiveProcessor;

    @Inject
    StructuredPaymentProcessor structuredProcessor;

    public record ComparisonResult(
        TransactionResult reactiveResult,
        TransactionResult structuredResult,
        long reactiveTotalTime,
        long structuredTotalTime,
        long performanceDifference
    ) {}

    @POST
    public ComparisonResult compare(TransactionRequest request) throws ExecutionException, InterruptedException {
        logger.info("⚖️  Running PERFORMANCE COMPARISON");

        // Run reactive
        long reactiveStart = System.currentTimeMillis();
        TransactionResult reactiveResult = reactiveProcessor.processTransaction(request).get();
        long reactiveTime = System.currentTimeMillis() - reactiveStart;

        // Run structured
        long structuredStart = System.currentTimeMillis();
        TransactionResult structuredResult = structuredProcessor.processTransaction(request);
        long structuredTime = System.currentTimeMillis() - structuredStart;

        long difference = structuredTime - reactiveTime;

        logger.info(String.format("📊 Reactive: %dms | Structured: %dms | Difference: %+dms%n",
            reactiveTime, structuredTime, difference));

        return new ComparisonResult(
            reactiveResult,
            structuredResult,
            reactiveTime,
            structuredTime,
            difference
        );
    }
}
