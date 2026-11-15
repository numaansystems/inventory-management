# inventory-management

Inventory, order and invoice management for a spice distribution business.

The backend is a Spring Boot 4.1 application (Java 21, Gradle, PostgreSQL). The web UI in `frontend/` is a
React + Vite + TypeScript app. Both currently cover products and stock; orders and invoices are coming in
later changes. There is no authentication yet: run it on your own machine only.

## Running locally

```bash
docker compose up -d                                        # PostgreSQL on localhost:5432 (db/user/password: inventory)
./gradlew bootRun --args='--spring.profiles.active=local'   # API on http://localhost:8080, with a starter catalogue
./gradlew build                                             # compile and run all tests (in-memory H2, no database needed)

cd frontend
npm install
npm run dev                                                 # UI on http://localhost:5173
```

The Vite dev server forwards `/api` to `http://localhost:8080` (override with `BACKEND_URL`), so the backend
needs no CORS configuration. Override the database with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

The `local` profile seeds twelve products named after the Bumi Spices storefront the first time it runs
(`BumiCatalogueSeeder`). Their SKUs and 100 g pack sizes are placeholders: edit them in the UI or in the seeder.
Without the profile the app starts with an empty catalogue.

## Web UI

- **Stock**: summary tiles, search and filters, and per-product Receive / Sell / Adjust and movement history.
- **Products**: create, edit, activate/deactivate and delete products, with categories.

Colours live in `frontend/src/theme.css` as CSS variables. The current palette is provisional and has not been
matched to bumispices.com yet; changing that one file restyles the whole app.

## API overview

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/products` | Create a product (starts with zero stock) |
| `GET` | `/api/products?active=&category=&page=&size=&sort=` | List products |
| `GET` | `/api/products/categories` | Catalogue categories (`code`, `label`) |
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
  -d '{"sku":"TUR-100G","name":"Turmeric powder","category":"SPICES_SEASONINGS","unit":"GRAM","packSize":100}'

curl -X POST localhost:8080/api/products/1/stock/movements -H 'Content-Type: application/json' \
  -d '{"type":"RECEIVE","quantity":20,"reference":"PO-1001"}'
```

Stock quantities are whole packs. Errors are returned as `application/problem+json` with a `code` field
(for example `VALIDATION_FAILED`, `NOT_FOUND`, `DUPLICATE_SKU`, `INSUFFICIENT_STOCK`).
