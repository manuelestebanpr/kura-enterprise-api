# KURA Architecture & Feature Manual

> **Version:** 0.1.0-SNAPSHOT (Sprint 0 / MVP)  
> **Generated:** 2026-02-17  
> **Repository:** `kura-enterprise-api`  
> **Stack:** Spring Boot 4.0.0 · Java 25 · PostgreSQL 16+ · Redis · Flyway  

---

## 1. Architecture Overview

### 1.1 Clean Architecture Layers

```
co.com.kura.enterprise/
├── api/                    # Presentation Layer
│   ├── controller/         # REST Controllers (6)
│   └── dto/                # Request/Response DTOs (12)
├── config/                 # Cross-cutting Configuration (3)
├── domain/                 # Domain Layer
│   ├── entity/             # JPA Entities (14)
│   └── repository/        # Spring Data Repositories (14)
├── infrastructure/         # External Integrations (8 — 4 interfaces, 4 mocks)
└── service/                # Business Logic (6)
```

### 1.2 Design Principles

| Principle | Implementation |
|-----------|---------------|
| **Stateless** | No JVM sessions; JWT-ready (placeholder token in MVP) |
| **DRY/SOLID** | Interface-based providers, single-responsibility services |
| **Soft Delete** | `deleted_at` field on all domain entities; queries filter `deletedAtIsNull` |
| **Audit Trail** | Immutable `audit_logs` table (Colombia compliance — 15yr retention) |
| **Tenant Isolation** | PoS-scoped queries (`WHERE pos_id = ?`) throughout |

### 1.3 Dual-Backend Architecture (Target)

| API | Traffic | Repository |
|-----|---------|-----------|
| `kura-b2c-api` | Patient commerce (high traffic) | Separate repo (Phase 2) |
| `kura-enterprise-api` | Lab admin, backoffice, B2B (this repo) | ✅ Implemented |

Both share the same database schema and `users` table for SSO.

---

## 2. Database Schema

### 2.1 Flyway Migrations

| Version | File | Tables |
|---------|------|--------|
| V1 | `V1__foundation_tables.sql` | `users`, `laboratories`, `points_of_service`, `warehouse_inventory`, `audit_logs` |
| V2 | `V2__catalog_tables.sql` | `master_services`, `bundle_items`, `lab_offerings`, `test_dependencies` |
| V3 | `V3__commerce_tables.sql` | `orders`, `order_items`, `walkin_tickets`, `payments` |
| V4 | `V4__results_and_shares.sql` | `patient_results`, `share_links` |

### 2.2 Entity Relationship Summary

```
Laboratories (1) ──→ (N) Points_of_Service (1) ──→ (1) Warehouse_Inventory
                                  │
                                  ├──→ (N) Lab_Offerings ──→ Master_Services
                                  ├──→ (N) Orders ──→ (N) Order_Items
                                  │         │
                                  │         ├──→ (1) Walkin_Tickets
                                  │         ├──→ (N) Payments
                                  │         └──→ (N) Patient_Results ──→ (N) Share_Links
                                  │
Users ────────────────────────────┘ (belongs to lab/pos via FK)

Master_Services (Composite Pattern: SINGLE | BUNDLE)
    ├──→ Bundle_Items (BUNDLE → SINGLE links)
    └──→ Test_Dependencies (BOM: what inventory a test consumes)
```

### 2.3 PostgreSQL Extensions

- **`uuid-ossp`** — UUID v4 primary key generation
- **`pg_trgm`** — Trigram-based fuzzy text search (indexed on `users.full_name`, `laboratories.name`, `points_of_service.name`, `master_services.name`)

---

## 3. API Endpoints

### 3.1 Auth (`/api/v1/auth`) — AuthController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/otp/send` | Send OTP to email (Redis, 5min TTL) | Public |
| POST | `/otp/verify` | Verify OTP code | Public |
| POST | `/register` | Register patient (RNEC mock + Ley 1581) | Public |
| POST | `/login` | Login (BCrypt verification) | Public |
| POST | `/password/reset` | Request password reset (Mock SES) | Public |
| POST | `/password/reset/confirm` | Confirm reset with token | Public |

### 3.2 Catalog (`/api/v1/catalog`) — CatalogController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/services` | Create service (SINGLE/BUNDLE) | Protected |
| GET | `/services/{code}` | Get service by code | Protected |
| GET | `/services/search?q=&limit=` | pg_trgm fuzzy search | Protected |
| GET | `/services/type/{type}` | List by type (SINGLE/BUNDLE) | Protected |
| POST | `/offerings` | Create lab offering (PoS + price) | Protected |

### 3.3 Commerce (`/api/v1/commerce`) — CommerceController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/orders` | Create order (guest or user, single-PoS cart) | Protected |
| GET | `/orders/{orderNumber}` | Get order by number | Protected |
| GET | `/orders/user/{userId}` | Get user's order history | Protected |

### 3.4 Results (`/api/v1/results`) — ResultController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/results/{id}/sample-taken` | Mark sample taken (triggers BOM stock deduction) | Protected |
| POST | `/results/{id}/complete` | Complete result with data | Protected |
| POST | `/results/{id}/audio` | Upload audio note (Mock S3) | Protected |
| POST | `/results/{id}/share` | Create 48h share link | Protected |
| GET | `/share/{shareUuid}` | Access result via share link | **Public** |
| GET | `/results/order/{orderId}` | Results by order | Protected |
| GET | `/results/patient/{patientId}` | Results by patient | Protected |

### 3.5 Import (`/api/v1/import`) — ImportController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/patients` | CSV patient import (multipart, dynamic column mapping) | Protected |

### 3.6 Health (`/api/v1`) — HealthController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/ping` | Service health check | Public |

---

## 4. Security

### 4.1 Configuration

- **Session Policy:** `STATELESS` (no server-side sessions)
- **CSRF:** Disabled (API-only, no browser forms)
- **Password Hashing:** BCrypt via `PasswordEncoder`
- **CORS:** Configurable origins (`kura.cors.allowed-origins`), credentials allowed
- **Public Endpoints:** `/api/v1/auth/**`, `/actuator/health`, `/actuator/info`, `/api/v1/share/**`

### 4.2 Cookie-Based SSO (Design — Phase 2)

- Root-domain scoped cookie: `Domain=.kura.com.co; HttpOnly; Secure; SameSite=Lax`
- Unified `users` table shared across `b2c-api` and `enterprise-api`
- JWT generation placeholder in `AuthService.login()` (returns `"placeholder-jwt-token"`)

### 4.3 Colombia Compliance (Ley 1581)

- `consent_ley1581` + `consent_date` required at registration
- Registration blocked if consent is `false`
- `cedula` and `full_name` locked after RNEC verification (immutable by design)
- All state changes logged to `audit_logs` (immutable — no UPDATE/DELETE)

---

## 5. Key Business Logic

### 5.1 OTP Flow (OtpService)

1. Generate 6-digit code → Store in Redis (`otp:{email}`, 5min TTL)
2. Send via Mock SES email
3. Verify: compare against Redis, delete on success

### 5.2 Registration Flow (AuthService)

1. Validate Ley 1581 consent
2. Check cedula/email uniqueness
3. RNEC identity verification (mocked → always MATCH)
4. BCrypt hash password
5. Save user + audit log

### 5.3 Composite Catalog (CatalogService)

- **SINGLE** services: atomic lab tests
- **BUNDLE** services: composite of multiple SINGLEs via `bundle_items`
- Custom services trigger "Not Recommended" warning log
- `pg_trgm` fuzzy search with `similarity()` ranking

### 5.4 Commerce Flow (OrderService)

1. Cart restricted to **single PoS** per order
2. Price lookup: PoS-specific (`lab_offerings`) → fallback to `base_price`
3. Walk-in ticket generated: `expires_at = NOW() + 15 days`
4. MercadoPago (mocked): creates preference ID + checkout URL

### 5.5 Stock Deduction on SAMPLE_TAKEN (ResultService)

1. `markSampleTaken()` → set status, timestamp
2. Look up BOM via `test_dependencies` for the service
3. Deduct inventory at the PoS (`warehouse_inventory`)
4. Log warnings for negative stock or low threshold breaches

### 5.6 Share Links (ResultService)

- UUID-based public URLs: `/share/{uuid}`
- 48-hour expiry enforced at access time
- Access count tracked per link

---

## 6. Internationalization (i18n)

| File | Language |
|------|----------|
| `src/main/resources/i18n/messages_es.properties` | Spanish (Colombia) — Primary |
| `src/main/resources/i18n/messages_en.properties` | English |

Coverage: auth messages, error messages, CRUD operations, inventory alerts.

---

## 7. Registry of Mocked Integrations

> ⚠️ **All entries below are placeholders.** Each must be replaced with real implementations in Phase 2.

| # | System | Interface | Mock Implementation | File Path | Default Behavior | Phase 2 Action |
|---|--------|-----------|-------------------|-----------|-----------------|----------------|
| 1 | **AWS SES** (Email) | `EmailProvider` | `MockSesEmailProvider` | `infrastructure/MockSesEmailProvider.java` | Logs email content to console | Integrate AWS SES SDK |
| 2 | **RNEC** (Colombian ID Registry) | `IdentityVerificationProvider` | `MockRnecProvider` | `infrastructure/MockRnecProvider.java` | Always returns `MATCH` | Integrate RNEC SOAP/REST API |
| 3 | **MercadoPago** (Payments) | `PaymentProvider` | `MockMercadoPagoProvider` | `infrastructure/MockMercadoPagoProvider.java` | Returns fake preference ID + sandbox URL | Integrate MercadoPago SDK for COP |
| 4 | **AWS S3** (File Storage) | `StorageProvider` | `MockS3StorageProvider` | `infrastructure/MockS3StorageProvider.java` | Saves files to `/tmp/kura-storage` | Integrate AWS S3 SDK |

All mocked files contain the comment: `// MOCK INTEGRATION: [System Name] - [Description]`

---

## 8. Configuration

### 8.1 application.yml

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/kura` | PostgreSQL connection |
| `spring.data.redis.host` | `localhost` | Redis for OTP + password reset tokens |
| `spring.flyway.locations` | `classpath:db/migration` | Flyway migration path |
| `server.port` | `8080` | API port |
| `kura.cors.allowed-origins` | `http://localhost:4200,4201` | CORS origins |

### 8.2 Environment Variables

| Variable | Purpose |
|----------|---------|
| `DB_USERNAME` | PostgreSQL username (default: `kura`) |
| `DB_PASSWORD` | PostgreSQL password (default: `kura`) |
| `REDIS_HOST` | Redis host (default: `localhost`) |
| `REDIS_PORT` | Redis port (default: `6379`) |

---

## 9. Project Statistics

| Metric | Count |
|--------|-------|
| Java source files | 68 |
| JPA entities | 14 |
| Spring Data repositories | 14 |
| REST controllers | 6 |
| REST endpoints | 24 |
| DTOs | 12 |
| Services | 6 |
| Flyway migrations | 4 |
| Database tables | 14 |
| Mocked integrations | 4 |
| i18n bundles | 2 (ES/EN) |

---

## 10. Build & Run

```bash
# Prerequisites: Java 25, PostgreSQL 16+, Redis
mvn clean compile                          # Compile
mvn spring-boot:run                        # Run (needs DB + Redis)
mvn test                                   # Test

# Environment setup
export DB_USERNAME=kura DB_PASSWORD=kura
export REDIS_HOST=localhost REDIS_PORT=6379
```

---

*Generated by Opus 4.6 — KURA Ecosystem Architect*
