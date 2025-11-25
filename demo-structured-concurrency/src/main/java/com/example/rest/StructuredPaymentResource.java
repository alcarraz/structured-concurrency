package com.example.rest;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.FailFastStructuredPaymentProcessor;
import com.example.structured.StructuredPaymentProcessor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/structured")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StructuredPaymentResource {

    @POST
    @Path("/normal")
    public TransactionResult processNormal(TransactionRequest request) throws InterruptedException {
        StructuredPaymentProcessor processor = new StructuredPaymentProcessor();
        return processor.processTransaction(request);
    }

    @POST
    @Path("/fail-fast")
    public TransactionResult processFailFast(TransactionRequest request) throws InterruptedException {
        FailFastStructuredPaymentProcessor processor = new FailFastStructuredPaymentProcessor();
        return processor.processTransaction(request);
    }
}
