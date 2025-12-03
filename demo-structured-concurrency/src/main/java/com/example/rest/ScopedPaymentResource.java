package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.scopedvalues.ScopedPaymentProcessor;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/scoped")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScopedPaymentResource {

    @Inject
    ScopedPaymentProcessor scopedProcessor;

    @POST
    @Path("/fail-fast")
    public TransactionResult processFailFast(TransactionRequest request) throws Exception {
        return scopedProcessor.processTransaction(request);
    }
}