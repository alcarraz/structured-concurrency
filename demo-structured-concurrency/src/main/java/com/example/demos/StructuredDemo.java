package com.example.demos;

import com.example.model.TransactionRequest;
import com.example.model.TransactionResult;
import com.example.structured.StructuredProcessor;
import com.example.structured.StructuredPaymentProcessor;
import com.example.structured.FailFastStructuredPaymentProcessor;
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

/**
 * Unified Structured Concurrency Demo
 * <p>
 * This demo showcases different structured concurrency approaches to concurrent transaction processing.
 * Supports different processor implementations based on command-line argument.
 * <p>
 * Run directly from IDE using JEP 512 simplified main method.
 */
public class StructuredDemo {
    private static final Logger logger = LogManager.getLogger(StructuredDemo.class);

    enum Type {
        NORMAL("Normal Structured Concurrency (Await All)"), FAIL_FAST("Fail-Fast Structured Concurrency");
        final String description;
        Type(String description) {
            this.description = description;
        }
    }
    public void main(String... args) throws InterruptedException {
        // Create CardRepository first
        CardRepository cardRepository = new CardRepository();

        // Create services (passing cardRepository to BalanceService)
        BalanceService balanceService = new BalanceService(cardRepository);
        CardValidationService cardValidationService = new CardValidationService();
        ExpirationService expirationService = new ExpirationService();
        PinValidationService pinValidationService = new PinValidationService();
        MerchantValidationService merchantValidationService = new MerchantValidationService();

        Type processorType = (args.length > 0 && args[0].equalsIgnoreCase("fail-fast")) ? Type.FAIL_FAST : Type.NORMAL;

        StructuredProcessor processor = createProcessor(processorType, balanceService,
                cardValidationService, expirationService, pinValidationService, merchantValidationService);

        logger.info("🚀 Running STRUCTURED CONCURRENCY Demo - {}", processorType.description);
        logger.info("═════════════════════════════════════════════════════════");

        // Use expired card for fail-fast demo, valid card for normal demo
        TransactionRequest request = createTestRequest(processorType);

        TransactionResult result = processor.processTransaction(request);
        DemoUtil.printResult(result);
    }

    private StructuredProcessor createProcessor(Type type, BalanceService balanceService,
                                                 CardValidationService cardValidationService,
                                                 ExpirationService expirationService,
                                                 PinValidationService pinValidationService,
                                                 MerchantValidationService merchantValidationService) {
        return switch (type) {
            case FAIL_FAST -> new FailFastStructuredPaymentProcessor(balanceService, cardValidationService,
                    expirationService, pinValidationService, merchantValidationService);
            case NORMAL -> new StructuredPaymentProcessor(balanceService, cardValidationService,
                    expirationService, pinValidationService, merchantValidationService);
        };
    }

    private TransactionRequest createTestRequest(Type type) {
        return switch (type) {
            case NORMAL -> new TransactionRequest(
                    "4532-1234-5678-9012", "1225", "1234",  // December 2025 (valid)
                new BigDecimal("100.00"), "Coffee Shop"
            );
            case FAIL_FAST -> new TransactionRequest(  // fail-fast case
                    "5555-4444-3333-2222", "1225", "9876",  // December 2023 (expired)
                new BigDecimal("75.00"), "Online Store"
            );
        };
    }
}
