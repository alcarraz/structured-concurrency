package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.reactive.ReactivePaymentProcessor;
import com.example.reactive.BasicReactivePaymentProcessor;
import com.example.reactive.ReactiveWithExceptionsPaymentProcessor;
import com.example.reactive.FixedReactiveFailFastPaymentProcessor;
import com.example.repository.CardRepository;
import com.example.services.BalanceService;
import com.example.services.CardValidationService;
import com.example.services.ExpirationService;
import com.example.services.MerchantValidationService;
import com.example.services.PinValidationService;
import com.example.utils.DemoUtil;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.example.fixtures.DemoCards.*;

/**
 * Unified Reactive Programming Demo
 * <p>
 * This demo showcases different reactive approaches to concurrent transaction processing.
 * Supports different processor implementations based on command-line argument.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class ReactiveDemo {
    private static final Logger logger = LogManager.getLogger(ReactiveDemo.class);

    enum Type {
        BASIC("Basic Reactive (CompletableFuture)"),
        EXCEPTIONS("Reactive with Exceptions"),
        FAIL_FAST("Fixed Reactive Fail-Fast");

        final String description;
        Type(String description) {
            this.description = description;
        }
    }

    public void main(String... args) {
        // Create CardRepository first
        CardRepository cardRepository = new CardRepository();

        // Create services (passing cardRepository to all card-aware services)
        BalanceService balanceService = new BalanceService(cardRepository);
        CardValidationService cardValidationService = new CardValidationService(cardRepository);
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        MerchantValidationService merchantValidationService = new MerchantValidationService();

        Type processorType = getProcessorType(args.length > 0 ? args[0] : "basic");

        ReactivePaymentProcessor processor = createProcessor(processorType, balanceService,
                cardValidationService, expirationService, pinValidationService, merchantValidationService);

        logger.info("🔄 Running REACTIVE Demo - {}", processorType.description);
        logger.info("═══════════════════════════════════════════");

        //Simulate failure when trying to demo fail fast behavior.
        String expDate = (processorType == Type.BASIC) ? VALID_CARD_EXPIRATION : EXPIRED_CARD_EXPIRATION;
        String cardNumber = (processorType == Type.BASIC) ? VALID_CARD_NUMBER : EXPIRED_CARD_NUMBER;
        String pin = (processorType == Type.BASIC) ? VALID_CARD_PIN : EXPIRED_CARD_PIN;

        // Valid transaction
        TransactionRequest validRequest = new TransactionRequest(
                cardNumber, expDate, pin,
            new BigDecimal("100.00"), "Coffee Shop"
        );

        try {
            TransactionResult result = processor.processTransaction(validRequest).get();
            DemoUtil.printResult(result);
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }

    private ReactivePaymentProcessor createProcessor(Type type, BalanceService balanceService,
                                                      CardValidationService cardValidationService,
                                                      ExpirationService expirationService,
                                                      PinValidationService pinValidationService,
                                                      MerchantValidationService merchantValidationService) {
        return switch (type) {
            case BASIC -> new BasicReactivePaymentProcessor(balanceService, cardValidationService,
                    expirationService, pinValidationService, merchantValidationService);
            case EXCEPTIONS -> new ReactiveWithExceptionsPaymentProcessor(balanceService, cardValidationService,
                    expirationService, pinValidationService, merchantValidationService);
            case FAIL_FAST -> new FixedReactiveFailFastPaymentProcessor(balanceService, cardValidationService,
                    expirationService, pinValidationService, merchantValidationService);
        };
    }

    private Type getProcessorType(String type) {
        return switch (type.toLowerCase()) {
            case "exceptions" -> Type.EXCEPTIONS;
            case "fail-fast" -> Type.FAIL_FAST;
            default -> Type.BASIC;
        };
    }
}
