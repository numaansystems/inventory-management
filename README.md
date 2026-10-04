# inventory-management

Inventory, order and invoice management for a spice distribution business.

The backend is a Spring Boot 4.1 application (Java 21, Gradle, PostgreSQL). It currently covers products and
stock; orders, invoices and a frontend are coming in later changes.

## Running locally

```bash
docker compose up -d     # PostgreSQL on localhost:5432 (db/user/password: inventory)
./gradlew bootRun        # API on http://localhost:8080
./gradlew build          # compile and run all tests (uses in-memory H2, no database needed)
```

Override the database with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

## API overview

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/products` | Create a product (starts with zero stock) |
| `GET` | `/api/products?active=&page=&size=&sort=` | List products |
| `GET` | `/api/products/{id}` | Get a product |
| `PUT` | `/api/products/{id}` | Replace a product |
| `DELETE` | `/api/products/{id}` | Delete a product with no stock history |
| `GET` | `/api/stock` | Current stock for all products |
| `GET` | `/api/products/{id}/stock` | Current stock for one product |
| `POST` | `/api/products/{id}/stock/movements` | Record a `RECEIVE`, `SELL` or `ADJUST` movement |
| `GET` | `/api/products/{id}/stock/movements` | Movement history, newest first |
| `GET` | `/actuator/health` | Health check |

Example:

```bash
curl -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
  -d '{"sku":"TUR-100G","name":"Turmeric powder","unit":"GRAM","packSize":100}'

curl -X POST localhost:8080/api/products/1/stock/movements -H 'Content-Type: application/json' \
  -d '{"type":"RECEIVE","quantity":20,"reference":"PO-1001"}'
```

Stock quantities are whole packs. Errors are returned as `application/problem+json` with a `code` field
(for example `VALIDATION_FAILED`, `NOT_FOUND`, `DUPLICATE_SKU`, `INSUFFICIENT_STOCK`).
