package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/structured")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StructuredPaymentResource {

    @Inject
    StructuredPaymentProcessor normalProcessor;

    @Inject
    FailFastStructuredPaymentProcessor failFastProcessor;

    @POST
    @Path("/normal")
    public TransactionResult processNormal(TransactionRequest request) throws InterruptedException {
        return normalProcessor.processTransaction(request);
    }

    @POST
    @Path("/fail-fast")
    public TransactionResult processFailFast(TransactionRequest request) throws InterruptedException {
        return failFastProcessor.processTransaction(request);
    }
}
