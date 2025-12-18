# Structured Concurrency & Scoped Values - Java 25

Material de demostración para JConf 2025

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Quarkus](https://img.shields.io/badge/Quarkus-3.26.3-blue?logo=quarkus)
![JEP 484](https://img.shields.io/badge/JEP%20484-Structured%20Concurrency-green)
![JEP 506](https://img.shields.io/badge/JEP%20506-Scoped%20Values-green)

## Acerca del Repositorio

Este repositorio contiene material de demostración para una presentación sobre **Structured Concurrency** y **Scoped Values** en Java 25.

**Contenido:**
- Aplicación Java con demos comparativos de diferentes enfoques de concurrencia
- [Presentación LaTeX con transparencias de la charla](presentacion/structured-concurrency-presentation.tex), [ver pdf](presentacion/structured-concurrency-presentation.pdf)

**Propósito:** Comparar y contrastar diferentes enfoques para manejar concurrencia en Java, desde programación reactiva tradicional (CompletableFuture) hasta las nuevas características de Java 25 (Structured Concurrency y Scoped Values).

**:information_source: Nota:** 
> Este README es trabajo en progreso, aún faltan secciones comentando el código auxiliar y explicación de la UI, puedes agregar este repositorio a tu watch list de GitHub (voy a notificar mediante una discusión cada vez que haga un commit relevante) o seguirme en [LinkedIn](https://www.linkedin.com/in/andresalcarraz/) para enterarte cuando lo vaya completando.
> 
> Esta versión permite entender los puntos de entrada a las clases que implementan la parte más relevante de la presentción.

---

## El Caso de Uso: Procesamiento de una transacción de compra

Para efectos de demostración, supongamos un caso simplificado de procesamiento de compras.  

Para validar la transacción hay varias cosas que se pueden hacer en paralelo, por ejemplo, la validación de los datos del comercio se pueden hacer en paralelo con la validación del consumidor.

En este flujo, asumimos que se necesita validar la tarjeta para obtener la cuenta que se usará para validar que tiene el saldo suficiente, el PIN asociado y la fecha de expiración. Cada parte la valida un servicio distinto.  

### Flujo de Validación

![Flujo de Validación](docs/flujo-validacion.png)

**Desafío:** ¿Cómo coordinamos estas validaciones paralelas de forma clara, segura y eficiente?

---

## Servicios de Validación

Cada validación del flujo está implementada por un/pl servicio especializado. Estos servicios simulan operaciones de red con retrasos definidos en constantes para poder observar el comportamiento de los diferentes enfoques de concurrencia.

**:information_source: Nota:** Si bien todos los servicios de validación del consumidor usan el mismo repositorio para simplificar el ejemplo. En la realidad es razonable que estén implementados por servicios diferentes, por ejemplo, uno de contabilidad para el saldo y un [HSM](https://es.wikipedia.org/wiki/HSM) para el PIN.
Lo más forzado es la fecha de vencimiento, que normalmente se guardaría en la misma entidad que la info de la tarjeta. Un ejemplo más apropiado sería la validación de CVV, que involucra también al HSM pero se puede ejecutar en paralelo con el PIN, pues es un comando separado.

### Servicios Involucrados

1. **MerchantValidationService**
   - **Propósito:** Valida el comercio, en el ejemplo, que no contenga `BLOCKED`
   - **Retraso simulado:** 500ms
   - **Implementación:** [`MerchantValidationService.java`](demo-structured-concurrency/src/main/java/com/example/services/MerchantValidationService.java)

2. **CardValidationService**
   - **Propósito:** Valida el número de tarjeta y recupera los datos de la cuenta
   - **Retraso simulado:** 100ms
   - **Retorna:** Objeto `Card` con saldo, PIN y fecha de vencimiento
   - **Implementación:** [`CardValidationService.java`](demo-structured-concurrency/src/main/java/com/example/services/CardValidationService.java)

3. **BalanceService**
   - **Propósito:** Valida saldo disponible e implementa bloqueo de fondos (two-phase commit)
   - **Retraso simulado:** 600ms (el más lento)
   - **Mecanismo especial:**
     - `validate()` - Valida el saldo y si tiene fondos, bloquea fondos agregándolos a transacciones pendientes
     - `releaseAmount()` - Libera fondos si otra validación falla, este método no tiene ningún efecto si no se bloqueó el fondo en la transacción actual
     - `transfer()` - Debita el saldo si todas las validaciones pasan
   - **Implementación:** [`BalanceService.java`](demo-structured-concurrency/src/main/java/com/example/services/BalanceService.java)

4. **ExpirationService**
   - **Propósito:** Valida que la tarjeta no esté vencida
   - **Retraso simulado:** 200ms
   - **Implementación:** [`ExpirationService.java`](demo-structured-concurrency/src/main/java/com/example/services/ExpirationService.java)

5. **PinValidationService**
   - **Propósito:** Valida que el PIN sea correcto
   - **Retraso simulado:** 300ms
   - **Implementación:** [`PinValidationService.java`](demo-structured-concurrency/src/main/java/com/example/services/PinValidationService.java)

### Modelo de Datos

![Diagrama de Models](docs/diagrama-models.png)

El modelo de datos está organizado en packages:
- **DTOs**: Request del cliente y su respuesta
- **Domain Model**: `Card` tiene la info necesaria para validar los datos de la tarjeta 
- **Validation Results**: Sealed interfaces para resultados (ValidationResult y CardValidationResult), en el caso de validación exitosa del número tarjeta, se devuelve la entidad con los datos de la tarjeta. 

### Interfaces y Servicios de Validación

![Diagrama de Servicios](docs/diagrama-servicios.png)

Los servicios de validación están organizados por interfaces:
- **ValidationService**: Para validaciones simples
- **CardAwareValidationService**: Para validaciones que requieren datos de tarjeta, se pasa la tarjeta obtenida por `CardValidationService`

---

## Procesadores Implementados

Este proyecto implementa el flujo anterior usando diferentes paradigmas de concurrencia. Los procesadores están organizados desde los más simples hasta los más sofisticados.
### Diagrama de clases

Todos los procesadores implementan el mismo flujo de validación pero con diferentes paradigmas de concurrencia:

![Diagrama de Procesadores](docs/diagrama-procesadores.png)

**Características de cada tipo:**

- **Reactive Processors (CompletableFuture)**:
    - [`BasicReactivePaymentProcessor`](#basicreactivepaymentprocessor): Espera todas las validaciones con `allOf()`
    - [`ReactiveWithExceptionsPaymentProcessor`](#nota-sobre-reactivewithexceptionspaymentprocessor): Muestra que la idea de fallo temprano automático de [`StructuredPaymentProcessor`](#failfaststructuredpaymentprocessor) no es tan simple para programación reactiva..
    - [`FixedReactiveFailFastPaymentProcessor`](#fixedreactivefailfastpaymentprocessor): Intenta implementar fallo temprano con programación reactiva, código más complejo.

- **Structured Processors (StructuredTaskScope)**:
    - [`StructuredPaymentProcessor`](#structuredpaymentprocessor): Espera todas con gestión automática del ciclo de vida
    - `FailFastStructuredPaymentProcessor`: Fail-fast automático al primer fallo, usando excepciones cuando los servicios no validan.

- **Scoped Processors (Scoped Values)**:
    - [`ScopedPaymentProcessor`](#scopedpaymentprocessor): Usa ScopedValue para propagación de contexto
    - Servicios scoped acceden al contexto sin necesidad de recibir todos los parámetros.

---
### Procesadores Básicos (Await-All)

Estos procesadores esperan a que **todas** las validaciones terminen, independientemente de si alguna de las paralelas falla antes.

#### BasicReactivePaymentProcessor

**Enfoque:** CompletableFuture (programación reactiva tradicional)

- **Archivo:** [`BasicReactivePaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/reactive/BasicReactivePaymentProcessor.java)
- **Qué hace:** Usa `CompletableFuture.allOf()` para ejecutar validaciones en paralelo
- **Demo CLI:** `./gradlew demoReactive`
- **Endpoint REST:** `/api/reactive/basic`

**Ejemplo de código:**
```java
// BasicReactivePaymentProcessor.java (líneas 54-90)
CompletableFuture<ValidationResult> merchantValidation =
    CompletableFuture.supplyAsync(() ->
        merchantValidationService.validate(request));

CompletableFuture<Card> cardValidation =
    CompletableFuture.supplyAsync(() ->
        cardValidationService.validate(request))
    .thenCompose(cardResult -> {
        // Validaciones paralelas anidadas...
    });

CompletableFuture.allOf(merchantValidation, cardValidation).join();
```

#### StructuredPaymentProcessor

**Enfoque:** Structured Concurrency (Java 25)

- **Archivo:** [`StructuredPaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/structured/StructuredPaymentProcessor.java)
- **Qué hace:** Usa `StructuredTaskScope.open()` para organizar tareas en jerarquía
- **Demo CLI:** `./gradlew demoStructured`
- **Endpoint REST:** `/api/structured/normal`
- **Líneas clave:** 63-97

**Ejemplo de código:**
```java
// StructuredPaymentProcessor.java (líneas 63-97)
try (var globalScope = StructuredTaskScope.open()) {
    Subtask<ValidationResult> merchantValidation =
        globalScope.fork(() ->
            merchantValidationService.validate(request));

    Subtask<CardValidationResult> cardValidation =
        globalScope.fork(() -> {
            // Scope anidado para validaciones del consumidor...
        });

    globalScope.join(); // Espera a TODAS las subtareas
}
```

---

### Procesadores Fail-Fast (Cancelación al Primer Fallo)

Estos procesadores cancelan todas las tareas restantes tan pronto como detectan un fallo.

#### Nota sobre ReactiveWithExceptionsPaymentProcessor

El procesador [`ReactiveWithExceptionsPaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/reactive/ReactiveWithExceptionsPaymentProcessor.java) añade manejo de excepciones a la versión reactiva básica, pero **no soluciona el problema fundamental**: cuando una tarea lanza una excepción, las demás tareas continúan ejecutándose hasta completarse. No hay cancelación automática.

- **Demo CLI:** `./gradlew demoReactiveExceptions`
- **Endpoint REST:** `/api/reactive/with-exceptions`

#### FixedReactiveFailFastPaymentProcessor

**Enfoque:** CompletableFuture con fail-fast MANUAL

- **Archivo:** [`FixedReactiveFailFastPaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/reactive/FixedReactiveFailFastPaymentProcessor.java)
- **Qué hace:** Implementa fail-fast manualmente con CompletableFuture
- **Complejidad:** Requiere ~80 líneas de código de coordinación para lograr cancelación al primer fallo
- **Demo CLI:** `./gradlew demoFixedReactiveFailFast`
- **Endpoint REST:** `/api/reactive/fail-fast`

#### FailFastStructuredPaymentProcessor

**Enfoque:** Structured Concurrency con fail-fast AUTOMÁTICO

- **Archivo:** [`FailFastStructuredPaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/structured/FailFastStructuredPaymentProcessor.java)
- **Qué hace:** Cancela automáticamente todas las tareas restantes al primer fallo
- **Ventaja:** La cancelación es automática, sin código de coordinación manual
- **Demo CLI:** `./gradlew demoStructuredFailFast`
- **Endpoint REST:** `/api/structured/fail-fast`
- **Líneas clave:** 67-103

**Ejemplo de código:**
```java
// FailFastStructuredPaymentProcessor.java (líneas 67-103)
try (var globalScope = StructuredTaskScope.open()) {
    globalScope.fork(() -> {
        ValidationResult result = merchantValidationService.validate(request);
        if (result instanceof ValidationResult.Failure(String msg)) {
            throw new ValidationException(msg); // → Cancela automáticamente el resto
        }
    });

    globalScope.join();
} catch (StructuredTaskScope.FailedException e) {
    // Manejo automático de fallo
}
```

**Comparación de demos:**
```bash
# Comparar comportamiento await-all vs fail-fast
./gradlew demoCompare

# Comparar específicamente el comportamiento en caso de fallo
./gradlew demoCompareFailure
```

---

### Propagación de Contexto con Scoped Values

**Ubicación:** `demo-structured-concurrency/src/main/java/com/example/scopedvalues/`

Los `ScopedValue` permiten propagar contexto a través de hilos virtuales sin necesidad de pasar parámetros explícitamente, evitando el "parameter drilling" (pasar el mismo parámetro a través de múltiples capas de llamadas).

#### ScopedPaymentProcessor

- **Archivo:** [`ScopedPaymentProcessor.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedPaymentProcessor.java)
- **Qué hace:** Usa `ScopedValue` para establecer contexto que los servicios pueden acceder sin recibir parámetros
- **Demo CLI:** `./gradlew demoScopedValues`
- **Endpoint REST:** `/api/scoped/fail-fast`
- **Líneas clave:** 21-22 (definición de ScopedValues), 44-111 (uso)

#### Servicios que acceden al contexto

Los siguientes servicios acceden al contexto vía `ScopedValue.get()`:
- [`ScopedCardValidationService.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedCardValidationService.java)
- [`ScopedBalanceService.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedBalanceService.java)
- [`ScopedExpirationService.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedExpirationService.java)
- [`ScopedPinValidationService.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedPinValidationService.java)
- [`ScopedMerchantValidationService.java`](demo-structured-concurrency/src/main/java/com/example/scopedvalues/ScopedMerchantValidationService.java)

#### Ejemplo de Código Scoped Values

```java
// ScopedPaymentProcessor.java (líneas 21-22)
// Definición de los ScopedValues
public static final ScopedValue<TransactionRequest> TRANSACTION_REQUEST =
    ScopedValue.newInstance();
public static final ScopedValue<Card> CARD =
    ScopedValue.newInstance();

// Establecer contexto (líneas 44-68)
ScopedValue.where(TRANSACTION_REQUEST, request).call(() -> {
    try (var globalScope = StructuredTaskScope.open()) {
        // Los servicios acceden vía TRANSACTION_REQUEST.get()
        createValidationTask(merchantValidationService, globalScope);
        // ...
    }
});

// ScopedBalanceService.java - Acceso al contexto sin parámetros
ValidationResult validate() {
    return super.validate(
        ScopedPaymentProcessor.TRANSACTION_REQUEST.get(),
        ScopedPaymentProcessor.CARD.get()
    );
}
```

---

## Inicio Rápido

### Ejecutar Demos desde Línea de Comandos

```bash
cd demo-structured-concurrency

# Reactive Programming
./gradlew demoReactive
./gradlew demoReactiveExceptions
./gradlew demoFixedReactiveFailFast

# Structured Concurrency
./gradlew demoStructured
./gradlew demoStructuredFailFast

# Scoped Values
./gradlew demoScopedValues

# Comparaciones
./gradlew demoCompare
./gradlew demoCompareFailure
```

### Ejecutar Aplicación Web (con Interfaz Gráfica)

```bash
cd demo-structured-concurrency
./gradlew quarkusDev

# Abrir en navegador: http://localhost:8080
```

### Probar Endpoints REST

```bash
curl -X POST http://localhost:8080/api/structured/fail-fast \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "1234-5678-9012-3456",
    "expirationDate": "1225",
    "pin": "1234",
    "amount": 100.00,
    "merchant": "Demo Store"
  }'
```

---

## Interfaz Web Interactiva

Durante la presentación se utilizó una **interfaz web interactiva** para demostrar las diferencias entre los enfoques de concurrencia en tiempo real.

**Para reproducir las demos:** http://localhost:8080 (después de ejecutar `./gradlew quarkusDev`)

### Vista General

![Vista inicial](docs/screenshots/01-vista-inicial.png)

La interfaz consta de dos paneles principales:

**Panel Izquierdo - Tarjetas de Prueba:**
- Lista de tarjetas precargadas con diferentes escenarios
- Cada tarjeta muestra: número parcial, balance, descripción
- Botones para usar, clonar o borrar tarjetas

**Panel Derecho - Formulario de Transacción:**
- Campos para datos de la transacción
- Dos selectores independientes de procesadores
- Permite comparar cualquier combinación de enfoques

**Tarjetas disponibles:**
- `4532...9012` - Válida ($5000) para transacciones exitosas
- `4111...1111` - Para demo de Scoped Values ($2000)
- `5555...2222` - **Expirada** ($1000) para demo de fail-fast
- `9876...7654` - Balance insuficiente ($500)

### Demo 1: Fail-Fast (54% Más Rápido)

Esta fue la **demostración principal** de la presentación, mostrando cómo structured concurrency cancela automáticamente tareas cuando detecta un fallo.

![Configuración fail-fast](docs/screenshots/02-formulario-configurado.png)

**Configuración utilizada:**
- Tarjeta `5555...2222` (expirada diciembre 2023)
- Procesador 1: "Reactive con Excepciones"
- Procesador 2: "Structured Fail-Fast"

![Resultados fail-fast](docs/screenshots/03-resultados-fail-fast.png)

**Resultados observados:**
- **Reactive:** 707ms - Esperó a que todas las validaciones completen
- **Structured:** 322ms - Canceló automáticamente al detectar expiración
- **Mejora:** 54% más rápido (385ms ahorro)

**¿Por qué la diferencia?**

El procesador reactive usa `CompletableFuture.allOf()` que espera a que **todas** las tareas completen:
- Expiración falla a ~200ms
- Pero continúa ejecutando: balance (600ms), PIN (300ms), merchant (500ms)
- Total: ~707ms

El procesador structured usa `StructuredTaskScope` con cancelación automática:
- Expiración falla a ~200ms
- Cancela inmediatamente las tareas restantes
- Total: ~322ms

**Conclusión:** Cancelación automática sin código adicional

### Demo 2: Transacción Exitosa (Rendimiento Comparable)

Esta demostración mostró que structured concurrency mantiene el rendimiento cuando todo funciona correctamente.

![Resultados exitosos](docs/screenshots/04-resultados-exitoso.png)

**Configuración:**
- Tarjeta válida `4532...9012`
- Monto: $100
- Procesadores: Basic (reactive) vs Normal (structured)

**Resultados:**
- **Reactive:** 704ms
- **Structured:** 710ms
- **Diferencia:** 1% (variación normal)

**Conclusión:** Structured concurrency ofrece código más simple sin sacrificar rendimiento en el caso exitoso.

### Demo 3: Scoped Values

Durante la presentación se demostró cómo Scoped Values permite propagar contexto automáticamente sin necesidad de pasar parámetros explícitos ("parameter drilling").

**Característica destacada:** Scoped Values es **ESTABLE en Java 25** (a diferencia de Structured Concurrency que está en 5ta preview).

### Reproducir las Demos

**Para el demo fail-fast:**
1. Ejecutar `./gradlew quarkusDev`
2. Abrir http://localhost:8080
3. Hacer click en tarjeta `5555...2222` → botón "Usar"
4. Seleccionar procesadores: "Con Excepciones" vs "Fail-Fast Automático"
5. Click en "⚖️ Comparar Procesadores"
6. Observar la diferencia de tiempo y el banner de mejora

**Para el demo exitoso:**
1. Usar tarjeta `4532...9012`
2. Seleccionar procesadores: "Basic" vs "Normal"
3. Observar rendimiento comparable

**Para Scoped Values:**
1. Usar cualquier tarjeta válida
2. Seleccionar "Fail-Fast con Scoped Values"
3. Click en "⚡ Ejecutar Único"
4. Observar logs en terminal mostrando propagación de contexto

---

## Código Auxiliar y Arquitectura de Soporte

> **⚠️ Nota Importante**
>
> El código de soporte (controladores REST, gestión de tarjetas, interfaz web) fue desarrollado mayormente con **asistencia de IA** para acelerar la creación de las demos. Este código **no representa una recomendación de arquitectura** para sistemas de producción.
>
> Se priorizó la **simplicidad y velocidad de desarrollo** sobre las mejores prácticas empresariales. Para sistemas financieros reales se requerirían patrones más robustos: persistencia real, seguridad, auditoría, manejo de errores exhaustivo, etc.
>
> **El código relevante para la presentación son los procesadores** (`reactive/`, `structured/`, `scopedvalues/`) y los servicios de validación.

### Controladores REST

La aplicación expone endpoints REST que conectan la interfaz web con los procesadores:

- **[StructuredPaymentResource](demo-structured-concurrency/src/main/java/com/example/rest/StructuredPaymentResource.java)** - Endpoints `/api/structured/*` (normal y fail-fast)
- **[ReactivePaymentResource](demo-structured-concurrency/src/main/java/com/example/rest/ReactivePaymentResource.java)** - Endpoints `/api/reactive/*` (basic, con excepciones, fail-fast)
- **[ScopedPaymentResource](demo-structured-concurrency/src/main/java/com/example/rest/ScopedPaymentResource.java)** - Endpoint `/api/scoped/fail-fast`
- **[ComparisonResource](demo-structured-concurrency/src/main/java/com/example/rest/ComparisonResource.java)** - Endpoint `/api/compare` (comparación lado a lado)
- **[CardResource](demo-structured-concurrency/src/main/java/com/example/rest/CardResource.java)** - CRUD de tarjetas `/api/cards/*`
- **[BalanceResource](demo-structured-concurrency/src/main/java/com/example/rest/BalanceResource.java)** - Consulta de saldos `/api/balance/*`

Los controladores actúan como **thin facade**: delegación directa a procesadores sin lógica de negocio en la capa REST.

### Componentes de Soporte

**Repositorio y Fixtures:**
- **[CardRepository](demo-structured-concurrency/src/main/java/com/example/repository/CardRepository.java)** - Almacenamiento en memoria (ConcurrentHashMap), no es base de datos real
- **[DemoCards](demo-structured-concurrency/src/main/java/com/example/fixtures/DemoCards.java)** - Tarjetas precargadas para escenarios de demo

**Utilidades:**
- **[DemoUtil](demo-structured-concurrency/src/main/java/com/example/utils/DemoUtil.java)** - Simulación de latencia de red (`simulateNetworkDelay()`)
- **[JacksonConfig](demo-structured-concurrency/src/main/java/com/example/config/JacksonConfig.java)** - Configuración JSON pretty-print

### Flujo de Integración

```
Interfaz Web (index.html)
         ↓ (fetch API)
   REST Controllers
         ↓ (delegación)
Payment Processors (reactive/structured/scoped)
         ↓ (orquestación)
   Validation Services
         ↓ (consulta)
    CardRepository (in-memory)
```

### Modelo de Datos

Todos los modelos son **records de Java** (inmutables, thread-safe):

- **[TransactionRequest](demo-structured-concurrency/src/main/java/com/example/model/TransactionRequest.java)** - Request del cliente
- **[TransactionResult](demo-structured-concurrency/src/main/java/com/example/model/TransactionResult.java)** - Response con métricas de rendimiento
- **[Card](demo-structured-concurrency/src/main/java/com/example/model/Card.java)** - Entidad de tarjeta
- **[ValidationResult](demo-structured-concurrency/src/main/java/com/example/model/ValidationResult.java)** - Sealed interface para resultados
- **[CardValidationResult](demo-structured-concurrency/src/main/java/com/example/model/CardValidationResult.java)** - Resultado de validación de tarjeta

---

## Requisitos

- **Java 25** con `--enable-preview` habilitado
- **Gradle** (wrapper incluido en el proyecto)

El proyecto ya está configurado para habilitar las preview features automáticamente en `build.gradle`.

---

## Estructura del Repositorio

```
jconf-structured-concurrency/
├── README.md                       # Este archivo
│
├── demo-structured-concurrency/    # Aplicación Java
│   ├── src/main/java/com/example/
│   │   ├── reactive/              # Procesadores CompletableFuture
│   │   │   ├── BasicReactivePaymentProcessor.java
│   │   │   ├── ReactiveWithExceptionsPaymentProcessor.java
│   │   │   └── FixedReactiveFailFastPaymentProcessor.java
│   │   │
│   │   ├── structured/            # Procesadores StructuredTaskScope
│   │   │   ├── StructuredPaymentProcessor.java
│   │   │   └── FailFastStructuredPaymentProcessor.java
│   │   │
│   │   ├── scopedvalues/          # Procesadores ScopedValue
│   │   │   ├── ScopedPaymentProcessor.java
│   │   │   └── Scoped*Service.java
│   │   │
│   │   ├── services/              # Servicios de validación
│   │   ├── rest/                  # REST controllers
│   │   ├── model/                 # Records (TransactionRequest, etc)
│   │   └── demos/                 # Clases ejecutables para demos CLI
│   │
│   ├── src/main/resources/
│   │   └── META-INF/resources/
│   │       └── index.html         # Interfaz websi
│   │
│   └── build.gradle
│
└── presentacion/                   # Presentación LaTeX
    ├── structured-concurrency-presentation.tex
    ├── Makefile
    └── sections/
```

---

## Contexto: Transacciones de Tarjetas en el Mundo Real

**🚧 Sección en desarrollo**

El caso de uso presentado simplifica el procesamiento de transacciones de tarjetas para fines didácticos. Esta sección explicará superficialmente cómo funcionan las transacciones reales y cómo se mapean los conceptos de la demo.

**Temas planeados:**
- **Actores principales:** POS (Terminal punto de venta), Adquiriente, Red de tarjetas (Visa/Mastercard), Emisor (banco)
- **Mensajería ISO 8583:** Formato estándar para transacciones financieras
- **Flujo simplificado:** POS → Adquiriente → Red → Emisor → respuesta reversa
- **Mapeo con la demo:**
  - Cómo los servicios de validación se corresponden con verificaciones reales del emisor
  - Dónde encajan las validaciones de comercio (adquiriente)
  - Qué representa el balance service en el contexto del emisor
  - Cómo el two-phase commit se relaciona con reversiones reales

*Pendiente de completar en una próxima actualización.*

---

## Licencia

Material educativo para JConf 2025.

**Autor:** Andrés Alcarraz
**Contacto:** alcarraz@gmail.com
