# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Demo project comparing traditional reactive programming (CompletableFuture) with Java 25's Structured Concurrency features through payment transaction validation scenarios.

## Build and Run Commands

### Build
```bash
./gradlew build              # Full build
./gradlew clean build        # Clean build
./gradlew compileJava        # Compile only
```

### Run Demos
All demos use Java 25 preview features and require `--enable-preview` flag (automatically configured).

```bash
# Main entry point with options
./gradlew run

# Specific demos via Gradle tasks
./gradlew demoReactive                    # CompletableFuture (basic, no fail-fast)
./gradlew demoReactiveExceptions          # Reactive with exceptions (still no fail-fast)
./gradlew demoFixedReactiveFailFast       # Manual fail-fast with CompletableFuture
./gradlew demoStructured                  # Structured Concurrency (await all)
./gradlew demoStructuredFailFast          # Structured Concurrency (fail-fast default)
./gradlew demoScopedValues                # Scoped Values demo
./gradlew demoCompare                     # Performance comparison
./gradlew demoCompareFailure              # Failure behavior comparison

# Run specific demo class directly
java --enable-preview -cp build/classes/java/main com.example.demos.BalanceLockingDemo
```

### Testing
```bash
./gradlew test               # Run all tests
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