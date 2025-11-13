# Script Detallado - Structured Concurrency Java 25 (45 minutos)

## Preparación Técnica
- **Terminal preparado** con: `cd demo-structured-concurrency`
- **Gradle listo**: `./gradlew build` ejecutado previamente
- **Pantalla dividida**: Código visible + terminal para demos
- **Comandos preparados**: Todos los `./gradlew demo*` listos para copy-paste
- **Timing**: Cronómetro visible para control de tiempo

---

## **[0:00-3:00] Introducción** (3 minutos)

### [0:00-1:00] Presentación Personal
"¡Hola a todos! Soy Andrés Alcarraz, uruguayo, ingeniero de sistemas, 25 años desarrollando en Java y especialista en jPOS en Cabal Uruguay.

Hoy vamos a explorar **Structured Concurrency** y **Scoped Values** en Java 25, dos features que nos devuelven a la simplicidad del código bloqueante tradicional **sin sacrificar performance**."

### [1:00-2:30] Evolución de la Concurrencia en Java
**[SLIDE: Evolución de la Concurrencia]**

"Primero, contexto histórico. La concurrencia en Java ha evolucionado constantemente:

- **Green Threads** (Java 1.0-1.1, 1995-1997): M:1 mapping
- **Platform Threads** (Java 1.2+, 1998): 1:1 con threads del OS
- **Executor Framework** (Java 5, 2004): Abstracción sobre thread pools
- **CompletableFuture** (Java 8, 2014): Programación reactiva, callbacks
- **Virtual Threads** (Java 21, 2023): M:N mapping, threads ligeros
- **Structured Concurrency** (Java 25, 2025): Quinta preview, coordinación estructurada"

### [2:30-3:00] Estado Actual en Java 25
**[SLIDE: Estado en Java 25]**

"¿Dónde estamos hoy?
- **Virtual Threads**: ✅ **Estables** desde Java 21
- **Scoped Values**: ✅ **Estables** en Java 25 - ¡adopten HOY!
- **Structured Concurrency**: Quinta preview - muy maduro, experimenten ahora"

---

## **[3:00-5:00] Virtual Threads: La Base** (2 minutos)

### [3:00-4:00] Arquitectura M:N
**[SLIDE: Virtual Threads M:N Mapping]**

"Antes de hablar de Structured Concurrency, necesitamos entender **por qué Virtual Threads lo hacen posible**.

**Platform Threads**: 1:1 mapping con OS threads
- ~2MB de stack memory por thread
- ~1ms para crear uno
- Límite práctico: miles

**Virtual Threads**: M:N mapping
- ~1KB de memoria
- ~1μs para crear uno
- Límite práctico: **millones**
- Scheduling inteligente: park/unpark automático"

### [4:00-5:00] Por qué Habilitan Structured Concurrency
**[SLIDE: La Base de Structured Concurrency]**

"Virtual Threads habilitan Structured Concurrency porque:

1. **Bajo Costo**: Crear miles de tareas ya no es problema
2. **Blocking is OK**: Código bloqueante simple es eficiente
3. **Natural Structure**: fork/join patterns son viables
4. **No thread pools**: Un thread por task, administración automática

**Sin Virtual Threads, Structured Concurrency sería impracticable.** Con ellos, es natural y eficiente."

---

## **[5:00-7:00] El Caso de Uso** (2 minutos)

### [5:00-7:00] Procesamiento de Transacción Financiera
**[SLIDE: Flujo de Transacción]**

"Caso de uso: **procesar una transacción financiera** con flujo optimizado:

**Paso 1 (Secuencial)**: Validar tarjeta primero (300ms)
- No tiene sentido validar más si la tarjeta no existe

**Paso 2 (Paralelo)**: Si tarjeta válida, validaciones concurrentes:
- ✅ Verificar saldo disponible (500ms)
- ✅ Validar PIN (400ms)
- ✅ Verificar fecha de expiración (200ms)

**Paso 3 (Secuencial)**: Transferir monto solo si todo pasó (500ms)

**Total caso exitoso**: 300 + max(500,400,200) + 500 = ~1300ms

Este es un **flujo secuencial-paralelo realista** que encontramos en sistemas reales."

---

## **[7:00-12:00] El Problema: Reactive Programming** (5 minutos)

### [7:00-9:00] Código CompletableFuture
**[SLIDE: ReactivePaymentProcessor código]**

"Tradicionalmente implementaríamos esto con CompletableFuture. Veamos el código..."

**[Explicar el código despacio]**
```java
return CompletableFuture
    .supplyAsync(() -> cardValidationService.validate(...))
    .thenCompose(cardResult -> {
        if (!cardResult.success())
            return CompletableFuture.completedFuture(failure);

        // Validaciones paralelas
        CompletableFuture<ValidationResult> balanceValidation = ...;
        return CompletableFuture.allOf(...)
            .thenCompose(_ -> {...});
    });
```

"Observen:
- Callbacks anidados con `thenCompose`
- Flujo condicional dentro de lambdas
- Manejo manual de la composición
- **80+ líneas de código** para la implementación básica"

### [9:00-10:30] Problemas del Approach
**[SLIDE: Problemas del Approach Reactivo]**

"Los problemas fundamentales:

1. **Callback Hell**: Lógica anidada difícil de seguir
2. **Manejo de Errores**: `CompletionException` wrapping, unwrapping manual
3. **Resource Management**: Difícil garantizar cleanup
4. **Debugging**: Stack traces confusos y fragmentados
5. **Cognitive Load**: El código **no lee como se ejecuta**"

### [10:30-12:00] Stack Traces y Complejidad
**[SLIDE EXTRA: Stack trace comparison o code metrics]**

"Métricas de complejidad:
- **Implementación básica**: ~80 líneas
- **Con fail-fast manual**: ~150 líneas
- **Nesting depth**: 3-4 niveles
- **Exception types**: CompletionException, ExecutionException, wrapping

**Resultado**: Código que funciona pero es difícil de mantener y extender."

---

## **[12:00-20:00] La Solución: Structured Concurrency** (8 minutos)

### [12:00-14:00] Código Structured
**[SLIDE: StructuredPaymentProcessor código]**

"Ahora Structured Concurrency - **mismo flujo, código elegante**..."

```java
// Paso 1: Validar tarjeta (secuencial)
ValidationResult cardResult = cardValidationService.validate(request.cardNumber());
if (!cardResult.success())
    return TransactionResult.failure(cardResult.message());

// Paso 2: Validaciones paralelas
try (var scope = StructuredTaskScope.open(Joiner.awaitAll())) {
    var balanceTask = scope.fork(() -> balanceService.validate(...));
    var pinTask = scope.fork(() -> pinValidationService.validate(...));
    var expirationTask = scope.fork(() -> expirationService.validate(...));

    scope.join(); // Espera todas

    // Verificar resultados...
}
// Paso 3: Transferir
ValidationResult transferResult = balanceService.transfer(...);
```

"¡El código lee **exactamente como se ejecuta**!"

### [14:00-16:00] Ventajas Clave
**[SLIDE: Ventajas de Structured Concurrency]**

"Ventajas fundamentales:

1. **Código Legible**: Lee de arriba a abajo
2. **Error Handling Simple**: Try-catch tradicional
3. **Resource Management**: Try-with-resources automático
4. **Debugging Claro**: Stack traces normales
5. **Performance Igual**: Misma ejecución paralela

**Métricas**:
- Implementación básica: **~50 líneas** (vs 80 reactive)
- Con fail-fast: **~40 líneas** (vs 150 reactive)
- Nesting depth: **1 nivel** (vs 3-4 reactive)"

### [16:00-17:30] Fail-Fast por Defecto
**[SLIDE: Fail-Fast example]**

"Lo mejor: fail-fast es **automático** con el joiner por defecto..."

```java
try (var scope = StructuredTaskScope.open()) {  // Sin joiner = fail-fast
    var task1 = scope.fork(() -> {
        if (!validation())
            throw new RuntimeException("Failed");
    });

    scope.join(); // Cancela automáticamente otras tareas al primer fallo
}
```

### [17:30-20:00] Principios Arquitecturales
**[SLIDE: Principios Clave]**

"Principios fundamentales de Structured Concurrency:

1. **Structured Lifecycle**: Tareas hijas mueren con el scope padre
2. **Fail-Fast Default**: `open()` cancela automáticamente en fallo
3. **Collect-All Opcional**: `open(awaitAll())` para esperar todas
4. **Observability**: Mejor monitoring y debugging
5. **Resource Safety**: Garantías automáticas de cleanup"

---

## **[20:00-32:00] DEMO BLOCK 1: Structured Concurrency** (12 minutos)

### [20:00-23:00] Demo 1 & 2: Reactive Básico y con Excepciones
**[TERMINAL]**
```bash
./gradlew demoReactive
```

"Demo 1 - CompletableFuture básico: ~520ms caso exitoso. Funciona pero código complejo."

**[TERMINAL]**
```bash
./gradlew demoReactiveExceptions
```

"Demo 2 - Con excepciones. **Ojo aquí**: Aunque falle una validación temprano...
→ **Sigue esperando TODAS las tareas** (~570ms)
→ ¡No hay fail-fast automático!"

### [23:00-26:00] Demo 3: Structured Básico
**[TERMINAL]**
```bash
./gradlew demoStructured
```

"Demo 3 - Structured Concurrency básico:
- Mismo resultado: ~520ms
- Código muchísimo más limpio
- Lee como se ejecuta"

### [26:00-30:00] Demo 4: Comparación Fail-Fast (KEY DEMO)
**[TERMINAL]**
```bash
./gradlew demoCompareFailure
```

"**Este es el momento clave**. Comparación lado a lado con fallo temprano:

→ **Reactive**: ~570ms (espera todas las tareas)
→ **Structured**: ~230ms (cancelación automática)

**¡60% más rápido en casos de error!**

**¿Por qué?**
- Reactive: `allOf()` no cancela, espera todas (~500+300+200+400ms)
- Structured: Fail-fast automático cancela inmediatamente (~200ms + overhead)

No es solo performance - es **mejor UX y menor uso de recursos**."

### [30:00-32:00] Demo 5: Caso Éxito
**[TERMINAL]**
```bash
./gradlew demoCompare
```

"Demo 5 - Comparación caso exitoso: **Paridad perfecta** (~520ms ambos).

Cuando todo va bien, misma performance. La diferencia está en:
1. **Código más simple**
2. **Fail-fast gratis** cuando algo falla"

---

## **[32:00-37:00] Features Avanzados** (5 minutos)

### [32:00-33:30] Custom Joiners
**[SLIDE: Custom Joiners]**

"Structured Concurrency ofrece varios joiners built-in:

- `allSuccessfulOrThrow()` - Fail-fast (default con `open()`)
- `awaitAll()` - Espera todas las tareas
- `anySuccessfulResultOrThrow()` - Primera tarea exitosa
- `allUntil(Instant)` - Todas hasta deadline

**Código ejemplo**: Esperar primeros 3 resultados de 10 tasks..."

### [33:30-34:30] Nested Scopes
**[SLIDE: Nested Scopes código]**

"Scopes se pueden anidar jerárquicamente:
- Scope externo para operaciones principales
- Scopes internos para sub-operaciones paralelas
- **Ventaja**: Cancela automáticamente TODOS los niveles si falla el padre"

### [34:30-35:30] Timeout Patterns
**[SLIDE: Timeout Patterns código]**

"Timeouts con deadlines:
```java
Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
try (var scope = StructuredTaskScope.open(Joiner.allUntil(deadline))) {
    // tareas...
    scope.join(); // Cancela todos si pasa el deadline
}
```"

### [35:30-37:00] Pitfalls & Best Practices
**[SLIDE: Common Pitfalls]**

"Errores comunes a evitar:

**❌ Anti-Pattern 1**: `synchronized` en Virtual Threads
- Problema: Pinning del carrier thread
- Solución: Usar `ReentrantLock`

**❌ Anti-Pattern 2**: Fork sin join
- Problema: Task leaks, resource leaks
- Solución: Siempre try-with-resources

**✅ Best Practice**: Exception handling dentro de cada fork si necesario"

---

## **[37:00-39:00] Comparación de Performance** (2 minutos)

### [37:00-38:00] Resumen Visual
**[SLIDE: Performance con gráfico de barras]**

"Resumen de performance:

**Caso Exitoso**:
- Reactive: ~520ms
- Structured: ~520ms
- **Resultado**: Paridad perfecta

**Caso Fallido**:
- Reactive: ~570ms (espera todas)
- Structured: ~230ms (fail-fast)
- **Resultado**: 60% más rápido"

### [38:00-39:00] Conclusión Performance
"La ventaja no es solo velocidad - es **arquitectura**:
- Código más simple
- Fail-fast automático
- Mismo API, diferentes estrategias
- Mejor developer experience sin trade-offs"

---

## **[39:00-45:00] Scoped Values** (6 minutos)

### [39:00-40:00] El Problema del Contexto
**[SLIDE: El problema de ThreadLocal]**

"Nuevo problema: **context propagation**.

**Problema tradicional**: Pasar `RequestContext` a cada método:
- Parameter drilling: pasar contexto por todos los niveles
- ThreadLocal: Problemático con Virtual Threads, no thread-safe, requiere cleanup manual

**Necesitamos**: Propagación automática, inmutable, thread-safe"

### [40:00-42:00] Scoped Values: La Solución
**[SLIDE: ScopedPaymentProcessor código]**

"Scoped Values resuelve esto elegantemente:

```java
public class ScopedPaymentProcessor {
    public static final ScopedValue<RequestContext> REQUEST_CONTEXT =
        ScopedValue.newInstance();

    public TransactionResult processTransaction(
            TransactionRequest request, RequestContext context) {
        return ScopedValue.where(REQUEST_CONTEXT, context).call(() -> {
            // TODO: procesamiento...
            // Todas las tareas forked heredan el contexto automáticamente
        });
    }
}
```

**Ventajas**:
- Definición: `ScopedValue.newInstance()`
- Set: `ScopedValue.where(...).call(() -> ...)`
- Access: `REQUEST_CONTEXT.get()` en cualquier parte
- **Inmutable, thread-safe, cleanup automático**"

### [42:00-43:00] Integración con Structured Concurrency
**[SLIDE: ScopedBalanceService código]**

"Las tareas forked heredan automáticamente el contexto:

```java
public class ScopedBalanceService {
    public ValidationResult validate(String cardNumber, BigDecimal amount) {
        // ¡Sin parámetro RequestContext!
        RequestContext context = REQUEST_CONTEXT.get();
        auditLog("Starting validation for " + cardNumber);
        // ...
    }
}
```

Sin Scoped Values necesitarías:
- Pasar `RequestContext` por parámetro a cada método
- O usar ThreadLocal (problemático)"

### [43:00-45:00] Estado y Adopción
**[SLIDE: Alert - Scoped Values ESTABLES]**

"**Scoped Values son ESTABLES en Java 25** (JEP 506 - FINAL)

✅ **Adopten HOY en producción**
- No más ThreadLocal
- No más parameter drilling
- Context propagation thread-safe automático
- Funciona perfectamente con Virtual Threads y Structured Concurrency

**Esto ya no es preview - es producción-ready.**"

---

## **[45:00-50:00] DEMO BLOCK 2: Scoped Values** (5 minutos)

### [45:00-50:00] Demo Scoped Values
**[TERMINAL]**
```bash
./gradlew demoScopedValues
```

"Demo final - Scoped Values en acción:

**Observen**:
1. Definición del `ScopedValue<RequestContext>`
2. Context set con `ScopedValue.where(...).call()`
3. Services acceden con `REQUEST_CONTEXT.get()` - ¡sin parámetros!
4. Audit logs muestran correlation ID automáticamente
5. Integración perfecta con Structured Concurrency

**Sin Scoped Values**: Necesitarías ~10 parámetros extra propagados por todos los métodos

**Con Scoped Values**: Acceso directo, limpio, inmutable

**Esto es ESTABLE - usenlo hoy.**"

---

## **[50:00-53:00] Conclusiones** (3 minutos)

### [50:00-51:30] Beneficios y Roadmap
**[SLIDE: ¿Por Qué Adoptar?]**

"Beneficios inmediatos:

1. **Legibilidad**: Código que lee como se ejecuta
2. **Mantenibilidad**: Más fácil de modificar y extender
3. **Debugging**: Stack traces claros
4. **Performance**: Misma velocidad en éxito, mejor en error
5. **Resource Safety**: Garantías automáticas

**Roadmap de Adopción**:
- ✅ **HOY**: Scoped Values en producción (ESTABLES)
- ⚠️ **AHORA**: Experimenten con Structured Concurrency (quinta preview - muy maduro)
- 🎯 **Java 26**: Structured Concurrency probablemente estable"

### [51:30-53:00] Mensajes Clave
**[SLIDE: Mensajes Clave]**

"Mensajes para llevar:

1. **Scoped Values ya son estables - adopta hoy**
2. **De vuelta a la simplicidad sin sacrificar performance**
3. **Structured Concurrency aún está en preview** - pero experimenten
4. **Código que lee como se ejecuta**
5. **Mejor developer experience sin trade-offs**

**Repo GitHub**: `https://github.com/alcarraz/structured-concurrency-java25`

**¡Gracias por su atención! Quedan 5-10 minutos para preguntas.**"

---

## **[53:00-55:00] Q&A** (5-10 minutos reservados)

---

## Notas para el Presentador

### Timing Crítico:
- **Presentación**: 50 minutos máximo
- **Q&A**: Mínimo 5 minutos reservados
- **Buffer**: 3 minutos para ajustes
- **Control**: Cronómetro visible durante presentación

### Pantalla:
- **Split view**: Código izquierda, terminal derecha para demos
- **Font size**: Grande para visibility
- **Tabs preparados**: Todos los archivos relevantes abiertos
- **Terminal listo**: En directorio correcto

### Secuencia de Demos:
**Demo Block 1 (SC)**:
1. `./gradlew demoReactive` (3 min)
2. `./gradlew demoReactiveExceptions` (3 min)
3. `./gradlew demoStructured` (3 min)
4. `./gradlew demoCompareFailure` (4 min) - **KEY DEMO**
5. `./gradlew demoCompare` (2 min)

**Demo Block 2 (SV)**:
6. `./gradlew demoScopedValues` (5 min)

### Key Moments:
- **[4:00] Virtual Threads habilitan SC**: Concepto fundamental
- **[12:00] Código lee como se ejecuta**: Ventaja principal
- **[26:00] 230ms vs 570ms**: Momento más impactante
- **[43:00] "ESTABLES en Java 25"**: Enfatizar Scoped Values adoption
- **[30:00] Fail-fast automático**: Diferenciador clave vs reactive

### Backup Plans:
- Si demos fallan: Screenshots preparados
- Si va largo: Omitir sección Advanced Features (slides 32-37)
- Si va corto: Ampliar Q&A o profundizar en demos
