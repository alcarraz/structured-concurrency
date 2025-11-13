# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Java demo project showcasing structured concurrency patterns, comparing traditional reactive programming (CompletableFuture) with Java's new Structured Concurrency features. The project demonstrates payment transaction validation scenarios.

## Build System
- **Build Tool**: Gradle with Java 25 preview features enabled
- **Build**: `./gradlew build`
- **Run Tests**: `./gradlew test` 
- **Clean Build**: `./gradlew clean build`
- **Dev Mode**: `./gradlew quarkusDev` (Quarkus development mode with hot reload)

## Architecture
The codebase demonstrates concurrent validation patterns through different approaches:

### Core Components
- `Transaction` - Record representing payment transactions
- `Validations` - Static validation methods (merchant, balance, PIN) with artificial delays
- `StopWatch` - Utility for measuring execution times

### Concurrency Patterns
1. **allof/**: Demonstrates "wait for all" patterns
   - `StructuredConcurrency.java` - Using Java's StructuredTaskScope.ShutdownOnFailure
   - `Reactive.java` - Using CompletableFuture.allOf()

2. **failfast/**: Demonstrates "fail fast" patterns  
   - `Reactive.java` - CompletableFuture with exception throwing

3. **anyof/**: Reserved for "any of" patterns (directory exists but empty)

### Key Dependencies
- Java 25 with preview features (--enable-preview flag required)
- Quarkus 3.15.1 framework
- Lombok for code generation
- SLF4J + Log4j2 for logging

## Development Notes
- All Java compilation, test execution, and runtime requires `--enable-preview` flag
- Each validation method includes artificial delays (1-3 seconds) to simulate real-world latency
- StopWatch utility tracks and prints execution times for performance comparison
- The project structure supports adding more concurrency pattern examples