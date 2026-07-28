# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

**Java version:** 21 (Corretto recommended; available via Gradle toolchain)

**Build system:** Gradle (with Spring Boot 3.x, Mockito, JUnit 5)

**Main class:** `com.hubsante.hub.HubApplication`

**Build & test:**
```bash
./gradlew build                    # Full build + tests + JaCoCo coverage
./gradlew test                     # Run all tests
./gradlew test --tests ConversionUtilsTest  # Run a single test
./gradlew spotlessApply            # Format code (Google Java Format, AOSP style)
./gradlew spotlessCheck            # Check formatting compliance
```

**Run locally:**
Before running, create configuration files:
```bash
cp dispatcher/src/main/resources/application-XXX.template.yaml dispatcher/src/main/resources/application-XXX.yaml
echo "client_id;useXML" > dispatcher/src/main/resources/client.preferences.csv
```

Set environment variables: `GITHUB_ACTOR` (GitHub username) and `GITHUB_TOKEN` (GitHub PAT with `repo` + `read:package`).

Then run with a specific vhost profile:
```bash
# HEALTH vhost (v1.5 → v2.x → v3.x)
./gradlew bootRun --args='--spring.profiles.active=local,local-15-15-v1,XXX'
./gradlew bootRun -PmodelVersion=2.4.0 --args='--spring.profiles.active=local,local-15-15-v2,XXX'
./gradlew bootRun -PmodelVersion=3.3.2 --args='--spring.profiles.active=local,local-15-15-v3,XXX'

# CISU vhost (NEXSIS)
./gradlew bootRun -PmodelVersion=3.3.2 --args='--spring.profiles.active=local,local-15-nexsis,XXX'
```

**Model version override:**
```bash
./gradlew build -PmodelVersion=3.3.2  # Default is 3.3.2; available via mavenLocal() during dev
```

## Module Purpose

The **Dispatcher** is a Spring Boot message broker that routes EDXL-wrapped health and emergency messages (SAMU, CISU/NEXSIS) across RabbitMQ virtual hosts. It:
- Deserializes and validates EDXL messages (XML/JSON)
- Routes messages based on sender/recipient prefixes and client perimeter subscriptions
- Performs version conversions (15-15_v1.5 ↔ 15-15_v2.0 ↔ 15-15_v2.1 ↔ 15-nexsis_v1.9)
- Persists messages and logs errors to MongoDB
- Publishes metrics (Prometheus) and error reports

Part of the larger **SAMU-Hub-Santé** project; sibling modules are under `/hub/` (RabbitMQ configuration, shared infrastructure).

## Architecture Overview

**Main packages:**
- `config/` — AMQP setup, Spring beans (mappers, WebClient, EdxlHandler), HubConfiguration (client preferences, vhost defaults)
- `service/` — Core business logic:
  - `Dispatcher.java` — RabbitMQ listeners (`@RabbitListener` on `dispatch` queue + dead-letter queue), publisher confirms
  - `MessageHandler.java` — Deserialization, validation, routing decisions, error handling
  - `ConversionHandler.java` — Calls external conversion-service WebClient for version/transcoding conversions
  - `MessagePersistenceService.java` — Logs to MongoDB
- `utils/` — Stateless utilities:
  - `ConversionUtils.java` — Enums (`ConversionType`, `RoutingType`), vhost routing logic, version mapping
  - `EdxlUtils.java` — EDXL envelope manipulation (extracting sender, recipient, use case)
  - `MessageUtils.java` — Hashing, recipient ID extraction, consistency checks
- `repository/` — MongoDB DAOs
- `model/` — Domain objects (wrappers, error reports)
- `exception/` — Custom exceptions (ConversionException, ValidationException, etc.)

**Dependencies:**
- **Spring Boot 3.x** (web, WebFlux, AMQP, MongoDB, actuator)
- **EDXL models library** (`com.hubsante:models:3.3.2` from GitHub Packages), provides `EdxlMessage`, `EdxlHandler`, `Validator`
- **RabbitMQ** (AMQP 0-9-1, producer confirms, DLX/DLQ pattern)
- **MongoDB** (persisting message logs, errors)
- **Jackson** (XML/JSON serialization; XmlMapper, ObjectMapper as Spring beans)
- **Micrometer/Prometheus** (metrics)
- **Testcontainers** (RabbitMQ, MongoDB for integration tests)

## Domain Concepts

**Perimeters** (administrative boundaries):
- `HEALTH` (15-15): SAMU, emergency medical dispatch
- `CISU` (15-nexsis): Fire/rescue coordination; uses NEXSIS protocol
- `GPS`, `SMUR`: Additional emergency service types

**vhost naming:** `<perimeter>_v<model-version>`, e.g. `15-15_v1.5`, `15-15_v2.0`, `15-nexsis_v1.9`.
Map in `Constants.CONVERSION_VHOST_MODEL`: vhost → model variant (`v1`, `v2`, `v3`).

**Actor ID prefixes:**
- `fr.health.*` — SAMU/health actors
- `fr.fire.*` — Fire service (CISU)
- `fr.cisu.*` — CISU coordination

**Routing types** (determined by sender/recipient prefixes in `ConversionUtils.determineRoutingType()`):
- `SAMU_TO_SAMU` — both HEALTH; may need version conversion on same perimeter
- `CISU_TO_SAMU` — CISU → HEALTH; requires transcoding + possible version conversion
- `SAMU_TO_CISU` — HEALTH → CISU; converts to NEXSIS vhost

**Conversion types**:
- `HEALTH_VERSION_CONVERSION` — upgrade/downgrade within HEALTH perimeter (v1.5 ↔ v2.0 ↔ v2.1)
- `CISU_VERSION_CONVERSION` — upgrade/downgrade within CISU perimeter (v1.9 variants)
- `CISU_TRANSCODING` — bridge HEALTH ↔ CISU protocol differences (incompatible message schemas)

## Conversion Subsystem

**Key utilities in `ConversionUtils`:**
- `determineConversionParameters()` — Inspects message + client config; returns `ConversionParametersDTO` with source/target versions, target vhost, conversion type
- `requiresVersionConversion()` — Boolean check if conversion available & needed
- `formatVersionToVhosts()` — Converts client-declared versions (`["1.5", "2.0"]`) to vhost array (`["15-15_v1.5", "15-15_v2.0"]`)
- `trimVersionSuffix()` — Strips `_v<version>` suffix from vhost name
- `isAlreadyCisuConverted()`, `requiresCisuConversion()` — State checks for CISU pathway

**Key classes:**
- `ConversionHandler` — Calls `conversionWebClient` (external conversion microservice) to perform actual message transformation
- `ConversionType` enum — `HEALTH_VERSION_CONVERSION`, `CISU_VERSION_CONVERSION`, `CISU_TRANSCODING`
- `RoutingType` enum — `SAMU_TO_SAMU`, `CISU_TO_SAMU`, `SAMU_TO_CISU`
- `ConversionParametersDTO` — Record holding `edxlMessage`, `sourceVersion`, `targetVersion`, `targetVhost`, `conversionType`

**Constants in `Constants.java`:**
```java
FR_HEALTH_PREFIX = "fr.health"          // SAMU actor ID prefix
FR_FIRE_PREFIX = "fr.fire"              // Fire service prefix
FR_CISU_PREFIX = "fr.cisu"              // CISU coordination prefix
NEXSIS_VHOST = "15-nexsis_v1.9"         // CISU standard vhost
HEALTH_VHOST_PREFIX = "15-15_v"         // HEALTH vhost prefix (e.g., "15-15_v2.0")

CONVERSION_VHOST_MODEL = Map.of(
    "15-15_v1.5", "v1",
    "15-15_v2.0", "v2",
    "15-15_v2.1", "v3",
    "15-nexsis_v1.9", "v3"
)                                       // Maps vhost → model variant
```

**Routing logic** (`ConversionUtils.determineRoutingType()`):
- Sender & recipient IDs determine path: both `fr.health.*` → SAMU_TO_SAMU; `fr.fire.*` → CISU; mixed → transcoding
- Each path applies different conversion rules (client perimeter subscriptions, available vhost versions, direct CISU flag)

## Testing

**Framework:** JUnit 5 (`@Test`, `@ParameterizedTest` with `@MethodSource`), Mockito (`@Mock`, `mockStatic()`), Testcontainers.

**Test locations:**
- `/src/test/java/com/hubsante/hub/service/` — Service tests, integration tests (RabbitMQ, MongoDB)
- `/src/test/java/com/hubsante/hub/utils/` — Utility tests

**Key test classes:**
- `ConversionUtilsTest` — Parameterized tests for version conversion logic, routing type determination
- `DispatcherTest` — RabbitMQ listener behavior
- `RabbitIntegrationTest`, `RabbitMQBatchTest` — End-to-end with Testcontainers (RabbitMQ + MongoDB)
- `LogIntegrityTest` — Message hash verification (input vs. output integrity)

**Run a single test:**
```bash
./gradlew test --tests ConversionUtilsTest
./gradlew test --tests "ConversionUtilsTest.testRequiresVersionConversion"
```

**JaCoCo code coverage** (auto-finalized after `test` task):
```bash
./gradlew test  # Generates report in build/reports/jacoco/test/html/
```

## Conventions

**Code style:**
- Google Java Format (AOSP style) enforced via Spotless
- License headers auto-added to all .java files by Spotless
- **Lombok** used heavily (`@Getter`, `@Setter`, `@Data`, `@Slf4j`, `@RequiredArgsConstructor`)

**Dependency injection:**
- Constructor injection preferred; Spring `@Autowired` used for optional beans
- `@Component`, `@Service` for bean registration

**Logging:**
- SLF4J with Logback (JSON format in production via logstash-logback-encoder)
- `StructuredLogger` wrapper for structured/tagged logging (used for metrics context)

**Message handling:**
- Messages deserialized to `EdxlMessage` objects via `EdxlHandler` (from models library)
- Validation via `Validator` (from models library)
- Errors wrapped in `ErrorWrapper` and sent to error reporting queue

**Metrics:**
- Micrometer `@Timed` annotations on Dispatcher listeners
- Custom counters published via `MeterRegistry` for errors, conversions, etc.

## Current Development

**Branch:** `dispatcher/verison-conversion-15-nexsis`

**Latest commit:** `feat(dispatcher): introduce conversion type enum` — Work-in-progress on version-conversion logic for 15-nexsis (CISU) vhost. The `ConversionType` enum unifies handling of HEALTH_VERSION_CONVERSION, CISU_VERSION_CONVERSION, and CISU_TRANSCODING paths.

**Next steps likely:** Integration with new conversion-service endpoints, handling edge cases in CISU → HEALTH routing, extending Testcontainers coverage.
