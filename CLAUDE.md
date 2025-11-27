# JConf 2024 - Structured Concurrency in Java 25

This repository contains materials for the JConf 2024 presentation on Structured Concurrency in Java 25, demonstrating the advantages of structured concurrency over traditional reactive programming patterns.

## Repository Structure

This project consists of two main components:

### 1. **demo-structured-concurrency/** - Java Demo Application
Quarkus-based Java 25 demo application with:
- Payment processing comparison demos (Reactive vs Structured Concurrency)
- REST API with interactive Web UI
- Standalone command-line demos
- Comprehensive architecture documentation

**See [`demo-structured-concurrency/CLAUDE.md`](demo-structured-concurrency/CLAUDE.md) for detailed documentation**

### 2. **presentacion/** - LaTeX Beamer Presentation
Conference presentation slides:
- LaTeX Beamer source files
- Makefile-based build system
- Continuous build support
- Presentation scripts (15min and 45min versions)

**See [`presentacion/CLAUDE.md`](presentacion/CLAUDE.md) for build workflow and validation**

## Quick Start

### Run Java Demos

```bash
# Start Quarkus application with Web UI
cd demo-structured-concurrency
./gradlew quarkusDev
# Open http://localhost:8080

# Or run standalone command-line demos
./gradlew demoReactive          # CompletableFuture approach
./gradlew demoStructured        # Structured Concurrency approach
./gradlew demoCompare           # Performance comparison
```

### Build Presentation

```bash
# Continuous build (auto-rebuild on changes)
cd presentacion
./continuous-build.sh

# Or one-time build
cd presentacion
make
```

## Key Technologies

- **Java 25** (with `--enable-preview`)
  - JEP 484: Structured Concurrency (5th Preview)
  - JEP 506: Scoped Values (Stable)
- **Quarkus 3.26.3** - Framework for Java demos
- **Log4J 2.24.3** - Logging framework
- **LaTeX Beamer** - Presentation framework

## Demo Scenarios

The Java application demonstrates payment transaction validation comparing:

1. **Traditional Reactive Programming** (`CompletableFuture`)
   - Complex callback chains and manual coordination
   - Waits for all validations even on failure (~570ms)
   - Requires ~80 lines for fail-fast behavior

2. **Structured Concurrency** (`StructuredTaskScope`)
   - Clean, linear code structure
   - Automatic fail-fast on first error (~230ms)
   - Natural exception propagation

**Performance difference on failure**: 60% faster with Structured Concurrency

## Workflow Integration

```
Java Demo Code → Referenced in Presentation → Live Demo During Talk
     ↓                      ↓                           ↓
 REST API           LaTeX .tex files           Interactive Web UI
```

1. Develop and test payment processing demos
2. Reference code examples in presentation slides
3. Use Web UI for live demonstration during talk

## Project Context

- **Event**: JConf 2024
- **Topic**: Structured Concurrency in Java 25
- **Focus**: Practical comparison showing architectural simplicity and performance benefits
- **Key Message**: "Back to simplicity without sacrificing performance"

## Directory Contents

```
.
├── CLAUDE.md                        # This file - repository overview
├── demo-structured-concurrency/    # Java demo application
│   ├── CLAUDE.md                   # Detailed Java project documentation
│   ├── build.gradle
│   ├── src/main/java/com/example/
│   └── src/main/resources/
├── presentacion/                   # LaTeX presentation
│   ├── CLAUDE.md                   # Presentation build workflow
│   ├── Makefile
│   ├── structured-concurrency-presentation.tex
│   ├── sections/
│   ├── SCRIPT-45MIN.md
│   └── SCRIPT-15MIN.md
└── out/                            # Build output directory
```

## For More Information

- Java demos and API documentation: [`demo-structured-concurrency/CLAUDE.md`](demo-structured-concurrency/CLAUDE.md)
- Presentation build and validation: [`presentacion/CLAUDE.md`](presentacion/CLAUDE.md)
