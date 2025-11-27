package com.example.rest;

import com.example.services.BalanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.Map;

@Path("/api/balance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BalanceResource {

    @Inject
    BalanceService balanceService;

    /**
     * GET /api/balance
     * Returns all card balances
     */
    @GET
    public Map<String, BigDecimal> getAllBalances() {
        return balanceService.getAllBalances();
    }

    /**
     * GET /api/balance/{cardNumber}
     * Returns balance for specific card
     */
    @GET
    @Path("/{cardNumber}")
    public BigDecimal getBalance(@PathParam("cardNumber") String cardNumber) {
        return balanceService.getBalance(cardNumber);
    }

    /**
     * PUT /api/balance/{cardNumber}
     * Updates balance for specific card
     */
    @PUT
    @Path("/{cardNumber}")
    public void updateBalance(@PathParam("cardNumber") String cardNumber, BigDecimal newBalance) {
        balanceService.setBalance(cardNumber, newBalance);
    }
}
