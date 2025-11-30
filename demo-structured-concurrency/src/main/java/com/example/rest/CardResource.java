package com.example.rest;

import com.example.model.Card;
import com.example.repository.CardRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/api/cards")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CardResource {

    @Inject
    CardRepository cardRepository;

    @GET
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @GET
    @Path("/{cardNumber}")
    public Response getCard(@PathParam("cardNumber") String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .map(card -> Response.ok(card).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response createCard(Card card) {
        if (cardRepository.exists(card.cardNumber())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Card already exists: " + card.cardNumber())
                    .build();
        }
        Card saved = cardRepository.save(card);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/{cardNumber}")
    public Response updateCard(@PathParam("cardNumber") String cardNumber, Card updatedCard) {
        // Validate card exists
        Optional<Card> existingCardOpt = cardRepository.findByCardNumber(cardNumber);
        if (existingCardOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Enforce immutability: reject if body tries to change card number
        if (updatedCard.cardNumber() != null && !updatedCard.cardNumber().equals(cardNumber)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Card number cannot be changed. Expected: " + cardNumber +
                            ", Received: " + updatedCard.cardNumber())
                    .build();
        }

        // Create updated card preserving the original card number (copy-on-write)
        Card cardWithCorrectNumber = new Card(
                cardNumber,
                updatedCard.expirationDate(),
                updatedCard.pin(),
                updatedCard.balance(),
                updatedCard.description()
        );

        Card saved = cardRepository.save(cardWithCorrectNumber);
        return Response.ok(saved).build();
    }

    @DELETE
    @Path("/{cardNumber}")
    public Response deleteCard(@PathParam("cardNumber") String cardNumber) {
        if (!cardRepository.exists(cardNumber)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        cardRepository.delete(cardNumber);
        return Response.noContent().build();
    }

    @POST
    @Path("/{cardNumber}/clone")
    public Response cloneCard(@PathParam("cardNumber") String cardNumber,
                              @QueryParam("newCardNumber") String newCardNumber) {
        if (newCardNumber == null || newCardNumber.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("newCardNumber query parameter is required")
                    .build();
        }

        if (cardRepository.exists(newCardNumber)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Card already exists: " + newCardNumber)
                    .build();
        }

        try {
            Card cloned = cardRepository.clone(cardNumber, newCardNumber);
            return Response.status(Response.Status.CREATED).entity(cloned).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }
}
