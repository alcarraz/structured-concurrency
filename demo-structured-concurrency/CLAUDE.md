# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Demo project comparing traditional reactive programming (CompletableFuture) with Java 25's Structured Concurrency features through payment transaction validation scenarios.

## Build and Run Commands

### Quarkus Application (REST API)

```bash
# Development mode with live reload
./gradlew quarkusDev

# Build the application
./gradlew build

# Run tests
./gradlew test
```

The application will start on http://localhost:8080

### Standalone Demo Tasks (Command Line)

You can also run demos directly from the command line without starting the Quarkus server:

```bash
./gradlew demoReactive                    # CompletableFuture (basic, no fail-fast)
./gradlew demoReactiveExceptions          # Reactive with exceptions (still no fail-fast)
./gradlew demoFixedReactiveFailFast       # Manual fail-fast with CompletableFuture
./gradlew demoStructured                  # Structured Concurrency (await all)
./gradlew demoStructuredFailFast          # Structured Concurrency (fail-fast default)
./gradlew demoScopedValues                # Scoped Values demo
./gradlew demoCompare                     # Performance comparison
./gradlew demoCompareFailure              # Failure behavior comparison
```

### REST API Endpoints

All endpoints accept and return JSON.

#### Health Check
```bash
GET /api/health
```

#### Reactive Processors
```bash
POST /api/reactive/basic              # Basic reactive (waits for all)
POST /api/reactive/with-exceptions    # Reactive with exception handling
POST /api/reactive/fail-fast          # Reactive with manual fail-fast
```

#### Structured Concurrency Processors
```bash
POST /api/structured/normal           # Structured (waits for all)
POST /api/structured/fail-fast        # Structured with automatic fail-fast
```

#### Comparison
```bash
POST /api/compare                     # Compare reactive vs structured performance
```

**Request Body Example:**
```json
{
  "cardNumber": "4532-1234-5678-9012",
  "expirationDate": "2512",
  "pin": "1234",
  "amount": 100.00,
  "merchant": "Test Merchant"
}
```

**Response Example:**
```json
{
  "success": true,
  "transactionId": "uuid-here",
  "amount": 100.00,
  "message": "Transaction processed successfully",
  "processedAt": "2025-11-24T20:00:00",
  "processingTimeMs": 245
}
```

### Testing with curl

```bash
# Health check
curl http://localhost:8080/api/health

# Test reactive basic
curl -X POST http://localhost:8080/api/reactive/basic \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4532-1234-5678-9012",
    "expirationDate": "2512",
    "pin": "1234",
    "amount": 100.00,
    "merchant": "Test Store"
  }'

# Compare performance
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4532-1234-5678-9012",
    "expirationDate": "2512",
    "pin": "1234",
    "amount": 50.00,
    "merchant": "Comparison Test"
  }'
```

### Web UI

The application includes an interactive Web UI for visual demos during presentations.

**Access the Web UI:**
```bash
# Start the application
./gradlew quarkusDev

# Open in browser
http://localhost:8080
```

**Features:**
- 💳 **Balance Panel**: View current balances for all cards in real-time
- 📝 **Transaction Form**: Submit transactions with preset scenarios
- 📊 **Comparison Panel**: Side-by-side results showing Reactive vs Structured performance
- 🎯 **Preset Scenarios**:
  - ✅ Valid Transaction (success case)
  - ⏰ Expired Card (fail-fast demo - 60% faster)
  - 💸 Insufficient Balance
  - 🔒 Invalid PIN

**Balance Management API:**
```bash
# Get all balances
curl http://localhost:8080/api/balance

# Get specific card balance
curl http://localhost:8080/api/balance/1234-5678-9012-3456

# Update balance (for demo purposes)
curl -X PUT http://localhost:8080/api/balance/1234-5678-9012-3456 \
  -H "Content-Type: application/json" \
  -d '1000.00'
```

## Architecture

### Package Structure
- **`model/`** - Domain records: `TransactionRequest`, `TransactionResult`, `ValidationResult`
- **`services/`** - Validation services implementing `ValidationService` interface
  - `BalanceService` - Balance validation with lock/unlock mechanism
  - `CardValidationService` - Card number validation
  - `ExpirationService` - Card expiration validation
  - `MerchantValidationService` - Merchant validation
  - `PinValidationService` - PIN validation
- **`reactive/`** - CompletableFuture-based processors
- **`structured/`** - StructuredTaskScope-based processors
- **`demos/`** - Runnable demo classes
- **`utils/`** - Utilities like `DemoUtil.simulateNetworkDelay()`

### Payment Processing Flow
All processors follow a multi-stage validation pattern:

1. **Merchant Validation** (PATH A) - Runs independently in parallel
2. **Consumer Validation** (PATH B) - Sequential then nested parallel:
   - Card validation (sequential prerequisite)
   - Balance + Expiration + PIN validations (parallel)
3. **Transfer** - Only executes if all validations pass

### Processor Implementations

#### Reactive Processors (CompletableFuture)
- **`BasicReactivePaymentProcessor`** - Waits for all validations to complete
- **`ReactiveWithExceptionsPaymentProcessor`** - Uses exceptions but still waits for all
- **`FixedReactiveFailFastPaymentProcessor`** - Manual fail-fast using `AtomicBoolean` coordination

#### Structured Concurrency Processors (StructuredTaskScope)
- **`StructuredPaymentProcessor`** - Uses `ShutdownOnSuccess` (awaits all)
- **`FailFastStructuredPaymentProcessor`** - Uses `ShutdownOnFailure` (automatic fail-fast)

### Balance Locking Mechanism
`BalanceService` implements a two-phase commit pattern:

1. **`validate(request)`** - Locks funds by adding to `pendingTransactions` set
2. **`releaseAmount(request)`** - Rollback: unlocks if validation fails elsewhere
3. **`transfer(request)`** - Commit: removes from pending and debits balance

**Thread Safety:**
- Per-card `ReentrantLock` protects balance operations
- `ConcurrentHashMap` for balances and pending transactions
- Idempotent `releaseAmount()` - safe to call even if never locked

**Key Implementation Detail:**
Uses `Set<TransactionRequest>` instead of tracking amounts to prevent double-release corruption.

## Exception Handling Pattern

### CompletableFuture Exception Conventions
**Always throw `RuntimeException` from within CompletableFuture async operations, NOT `CompletionException`.**

**Why:**
- `CompletionException(String)` constructor is **protected**, not public
- Only public constructor is `CompletionException(Throwable cause)` which requires double-wrapping
- CompletableFuture API automatically wraps any thrown exception in `CompletionException`
- Exception handlers use `throwable.getMessage()` to extract clean messages

**Example:**
```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Invalid card");  // Correct
    // NOT: throw new CompletionException(...)
})
.exceptionally(throwable -> {
    String reason = throwable.getMessage();  // Clean message extraction
    return TransactionResult.failure(reason, processingTime);
});
```

## Java 25 Preview Features Used
- **Structured Concurrency** (`StructuredTaskScope`) - Automatic subtask cancellation and lifecycle management
- **Scoped Values** - Thread-safe context propagation without ThreadLocal
- **Record patterns** and other preview language features

All compilation and execution tasks automatically include `--enable-preview` flag.