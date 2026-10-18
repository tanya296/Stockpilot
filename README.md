# StockPilot

A full-stack, multi-warehouse inventory and purchase-order platform.

Companies that move physical parts lose money in two ways: a part is out of stock in one location while it sits unused in another, and stock counts live in spreadsheets nobody trusts. StockPilot keeps one accurate, auditable record of every product in every warehouse, and tells managers what to reorder before it runs out.

**Example:** a car-parts network with depots in Chennai, Pune and Delhi. Chennai runs low on brake pads, so a manager transfers surplus from Pune. When stock is still short, they raise a purchase order to a supplier, and when the shipment arrives, one click receives it and updates every count.

## Features

| Role | Can do |
|---|---|
| **Staff** | View products, stock and history; record stock in and out |
| **Manager** | Everything Staff can, plus transfer stock between warehouses and manage purchase orders |
| **Admin** | Everything above, plus manage products and warehouses |

- JWT authentication with role-based access (`@PreAuthorize` and a role hierarchy)
- **Atomic stock transfers**: both sides of a transfer commit together or not at all (`@Transactional`)
- **Optimistic locking** (`@Version`): two users editing the same stock can't overwrite each other; the second gets HTTP 409
- **Stock never goes negative**: enforced in the service layer and again by a database `CHECK` constraint
- **Append-only ledger**: every change writes a `StockMovement`, so any stock level can be explained and audited
- **Purchase-order state machine**: `DRAFT → ORDERED → RECEIVED`, or `CANCELLED`; receiving adds stock in the same transaction
- Paginated, searchable, sortable product list; low-stock dashboard with a chart
- Consistent JSON errors from a global exception handler; Bean Validation on every request

## Tech stack

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA (Hibernate), Bean Validation |
| Security | Spring Security, JWT (jjwt), BCrypt |
| Database | PostgreSQL, Flyway migrations (H2 for tests) |
| API docs | springdoc OpenAPI (Swagger UI) |
| Frontend | React, TypeScript, Vite, Tailwind CSS, TanStack Query, Recharts |
| Tests | JUnit 5, Mockito, MockMvc, AssertJ |
| DevOps | Docker (multi-stage build), Docker Compose, GitHub Actions CI |

## Architecture

```
React (Vite)  ──HTTP + JWT──▶  Spring Boot API  ──JPA──▶  PostgreSQL
                                │
                                ├─ JwtAuthFilter   verifies the token on every request
                                ├─ Controllers     HTTP ↔ DTOs, @Valid, @PreAuthorize
                                ├─ Services        business rules, @Transactional
                                ├─ Repositories    Spring Data JPA queries
                                └─ Entities        mapped to tables created by Flyway
```

## Run locally

**Prerequisites:** Java 21, Node 18+, Docker Desktop.

```bash
# 1. Database
docker compose up -d db

# 2. Backend  (http://localhost:8090, Swagger at /swagger-ui.html)
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Or run the database and backend together in containers: `docker compose up -d --build`.

**Demo accounts** (password `Password@123`): `admin@stockpilot.dev`, `manager@stockpilot.dev`, `staff@stockpilot.dev`.

## Key API endpoints

| Method | URL | Who |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| GET | `/api/products?search=&category=&page=&size=&sort=` | Any user |
| POST / PUT / DELETE | `/api/products` | Admin |
| GET | `/api/products/{id}`, `/api/products/{id}/movements` | Any user |
| POST | `/api/stock/adjust` | Staff and above |
| POST | `/api/stock/transfer` | Manager and above |
| GET / POST | `/api/purchase-orders` | View: any user · Create: Manager and above |
| PATCH | `/api/purchase-orders/{id}/status` | Manager and above |
| GET | `/api/dashboard/summary`, `/api/dashboard/low-stock` | Any user |

Full interactive docs: `http://localhost:8090/swagger-ui.html`.

## Tests

```bash
cd backend && ./mvnw test
```

- **Unit tests**: the purchase-order state machine, JWT signing and tamper detection, and the stock service rules (with Mockito mocks)
- **Integration tests**: the full app against in-memory H2 via MockMvc, covering auth (401/403), validation (400), business rules (422), conflicts (409), transfers, the PO lifecycle and the dashboard

CI runs the backend tests, the frontend build and the Docker image build on every push.

## Future work

Payments, barcode scanning, email notifications for low stock, multi-tenant accounts, and splitting inventory and purchasing into separate services.
