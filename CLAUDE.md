# CLAUDE.md

Inventory, order and invoice management platform for a spice distribution business.
This repo currently holds the backend: products, stock levels and stock movements.
Orders, invoices and the frontend are planned follow-ups.

## Stack

- Java 21, Spring Boot 4.1.1 (Spring Framework 7, Hibernate 7, Jackson 3)
- Gradle 9 via the wrapper (`./gradlew`), Kotlin DSL (`build.gradle.kts`); dependency versions come from the Spring Boot BOM — don't pin versions it already manages
- Spring Web MVC, Spring Data JPA, Bean Validation, Actuator
- Flyway migrations in `src/main/resources/db/migration`; PostgreSQL at runtime, H2 (PostgreSQL mode) in tests
- JUnit 5, AssertJ, Mockito, MockMvc
- CI: `.github/workflows/build.yml` runs `./gradlew build` on every PR and on pushes to `main`

Boot 4 is modular: starters are per technology (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`,
`spring-boot-starter-webmvc-test`, ...) and test annotations moved packages
(e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`). Spring Data's
`PropertyReferenceException` lives in `org.springframework.data.core`.

## Commands

```bash
./gradlew build                 # compile + all tests; must pass before opening a PR
./gradlew test                  # tests only
./gradlew test --tests '*StockServiceTest'
./gradlew bootRun               # needs PostgreSQL, see below
docker compose up -d            # local PostgreSQL matching the default datasource settings
```

The app reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults: `jdbc:postgresql://localhost:5432/inventory`,
`inventory` / `inventory`). To run the test suite against a real PostgreSQL instead of H2, set
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` when invoking `./gradlew test`.

## Layout

Package-by-feature under `com.numaansystems.inventory`:

- `common` — error handling (`ApiExceptionHandler`), shared exceptions, `PageResponse`, `Clock` bean
- `product` — `Product` entity, CRUD service and `/api/products` controller
- `stock` — `StockLevel`, `StockMovement`, `StockMovementType`, `StockService` and stock endpoints

Each feature keeps entity, repository, service, controller and request/response records together.
Controllers, service constructors and response factory methods are package-private where possible.

## Domain rules

- A product is sold in packs: `packSize` (decimal, > 0) of `unit` (`GRAM`, `KILOGRAM`, `MILLILITRE`, `LITRE`, `PIECE`).
  SKUs are unique, 2–40 chars of `A-Z`, `0-9`, `-`.
- Stock is counted in whole packs (`long`). Every product has exactly one `stock_levels` row, created with the product.
- `StockService` is the only code allowed to change stock. Each change writes an append-only `stock_movements`
  row (signed `quantityChange`, `balanceAfter`) in the same transaction, so the ledger always sums to the level.
- Movement types: `RECEIVE` (+qty, qty > 0), `SELL` (−qty, qty > 0), `ADJUST` (signed, non-zero).
  Stock never goes negative (`INSUFFICIENT_STOCK`). Inactive products only accept `ADJUST`.
- Concurrent movements are serialised with a pessimistic row lock on the stock level (`findForUpdate`).
- Products with movements cannot be deleted (`PRODUCT_HAS_STOCK_HISTORY`); deactivate them instead.
- Future order/invoice code must go through `StockService.record(...)` rather than touching stock tables.

## API conventions

- JSON under `/api`; request/response bodies are Java records, never entities.
- Lists are paginated (`page`, `size`, `sort` query params) and return `PageResponse` (`content`, `page`, `size`,
  `totalElements`, `totalPages`).
- All errors are RFC 9457 `application/problem+json` with a machine-readable `code`:
  - `400 VALIDATION_FAILED` with `errors: [{field, message}]` (Bean Validation, or `InvalidRequestException`
    for cross-field rules); other framework 400s use `BAD_REQUEST`
  - `404 NOT_FOUND` (`NotFoundException`)
  - `409` business rules (`BusinessRuleException` subclasses, code set per rule) and `CONCURRENT_UPDATE`
- Timestamps are `Instant` (UTC, ISO-8601 in JSON), obtained from the injected `Clock`.

## Persistence conventions

- Schema changes only via new Flyway migrations `V<n>__<description>.sql`; never edit an applied migration.
  SQL must run on both PostgreSQL and H2 in PostgreSQL mode.
- `spring.jpa.hibernate.ddl-auto=validate`: entities must match the migrated schema exactly.
- `open-in-view` is off — load everything a response needs inside the service transaction.
- Entities use `@Version` for optimistic locking; money/measurements use `BigDecimal`.

## Testing conventions

- Unit tests for domain logic and services (plain JUnit + Mockito, fixed `Clock`).
- Integration tests extend `support.IntegrationTest` (`@SpringBootTest`, MockMvc, `test` profile with H2).
  They are not transactional; the base class empties tables before each test.
- Stock tests assert ledger reconciliation (sum of movements == level); keep that invariant covered.
