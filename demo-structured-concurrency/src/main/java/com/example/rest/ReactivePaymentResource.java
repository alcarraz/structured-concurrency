package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.reactive.FixedReactiveFailFastPaymentProcessor;
import com.example.reactive.ReactiveWithExceptionsPaymentProcessor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Path("/api/reactive")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactivePaymentResource {

    @POST
    @Path("/basic")
    public TransactionResult processBasic(TransactionRequest request) throws ExecutionException, InterruptedException {
        BasicReactivePaymentProcessor processor = new BasicReactivePaymentProcessor();
        CompletableFuture<TransactionResult> result = processor.processTransaction(request);
        return result.get();
    }

    @POST
    @Path("/with-exceptions")
    public TransactionResult processWithExceptions(TransactionRequest request) throws ExecutionException, InterruptedException {
        ReactiveWithExceptionsPaymentProcessor processor = new ReactiveWithExceptionsPaymentProcessor();
        CompletableFuture<TransactionResult> result = processor.processTransaction(request);
        return result.get();
    }

    @POST
    @Path("/fail-fast")
    public TransactionResult processFailFast(TransactionRequest request) throws ExecutionException, InterruptedException {
        FixedReactiveFailFastPaymentProcessor processor = new FixedReactiveFailFastPaymentProcessor();
        CompletableFuture<TransactionResult> result = processor.processTransaction(request);
        return result.get();
    }
}
