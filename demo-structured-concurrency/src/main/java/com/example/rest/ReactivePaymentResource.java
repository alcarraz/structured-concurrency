package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.reactive.FixedReactiveFailFastPaymentProcessor;
import com.example.reactive.ReactiveWithExceptionsPaymentProcessor;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Path("/api/reactive")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactivePaymentResource {

    @Inject
    BasicReactivePaymentProcessor basicProcessor;

    @Inject
    ReactiveWithExceptionsPaymentProcessor withExceptionsProcessor;

    @Inject
    FixedReactiveFailFastPaymentProcessor failFastProcessor;

    @POST
    @Path("/basic")
    public TransactionResult processBasic(TransactionRequest request) throws ExecutionException, InterruptedException {
        CompletableFuture<TransactionResult> result = basicProcessor.processTransaction(request);
        return result.get();
    }

    @POST
    @Path("/with-exceptions")
    public TransactionResult processWithExceptions(TransactionRequest request) throws ExecutionException, InterruptedException {
        CompletableFuture<TransactionResult> result = withExceptionsProcessor.processTransaction(request);
        return result.get();
    }

    @POST
    @Path("/fail-fast")
    public TransactionResult processFailFast(TransactionRequest request) throws ExecutionException, InterruptedException {
        CompletableFuture<TransactionResult> result = failFastProcessor.processTransaction(request);
        return result.get();
    }
}
