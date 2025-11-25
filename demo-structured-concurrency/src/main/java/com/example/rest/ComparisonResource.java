package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.ExecutionException;

@Path("/api/compare")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComparisonResource {

    public record ComparisonResult(
        TransactionResult reactiveResult,
        TransactionResult structuredResult,
        long reactiveTotalTime,
        long structuredTotalTime,
        long performanceDifference
    ) {}

    @POST
    public ComparisonResult compare(TransactionRequest request) throws ExecutionException, InterruptedException {
        System.out.println("⚖️  Running PERFORMANCE COMPARISON");

        // Run reactive
        BasicReactivePaymentProcessor reactiveProcessor = new BasicReactivePaymentProcessor();
        long reactiveStart = System.currentTimeMillis();
        TransactionResult reactiveResult = reactiveProcessor.processTransaction(request).get();
        long reactiveTime = System.currentTimeMillis() - reactiveStart;

        // Run structured
        StructuredPaymentProcessor structuredProcessor = new StructuredPaymentProcessor();
        long structuredStart = System.currentTimeMillis();
        TransactionResult structuredResult = structuredProcessor.processTransaction(request);
        long structuredTime = System.currentTimeMillis() - structuredStart;

        long difference = structuredTime - reactiveTime;

        System.out.printf("📊 Reactive: %dms | Structured: %dms | Difference: %+dms%n",
            reactiveTime, structuredTime, difference);

        return new ComparisonResult(
            reactiveResult,
            structuredResult,
            reactiveTime,
            structuredTime,
            difference
        );
    }
}
