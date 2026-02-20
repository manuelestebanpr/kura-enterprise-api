# KURA Architecture & Feature Manual

> **Version:** 0.2.0-SNAPSHOT (Sprint 0 / MVP — Full Ecosystem)  
> **Updated:** 2026-02-19  
> **Repositories:** `kura-enterprise-api` · `kura-b2c-api` · `kura-b2c-web` · `kura-workspace-web`  
> **Stack:** Spring Boot 4.0.0 · Java 25 · Angular 20 · PostgreSQL 16+ · Redis · Flyway · Tailwind CSS 4  

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Database Schema](#2-database-schema)
3. [Backend: kura-enterprise-api](#3-backend-kura-enterprise-api)
4. [Backend: kura-b2c-api](#4-backend-kura-b2c-api)
5. [Frontend: kura-b2c-web](#5-frontend-kura-b2c-web)
6. [Frontend: kura-workspace-web](#6-frontend-kura-workspace-web)
7. [Security & SSO](#7-security--sso)
8. [Colombia Compliance (Ley 1581)](#8-colombia-compliance-ley-1581)
9. [Key Business Logic](#9-key-business-logic)
10. [Internationalization (i18n)](#10-internationalization-i18n)
11. [Registry of Mocked Integrations](#11-registry-of-mocked-integrations)
12. [Configuration Reference](#12-configuration-reference)
13. [Project Statistics](#13-project-statistics)
14. [Known Issues & Tech Debt](#14-known-issues--tech-debt)
15. [Build & Run](#15-build--run)

---

## 1. Architecture Overview

### 1.1 System Topology

```
                           ┌──────────────────────────┐
                           │    PostgreSQL 16+ (kura)  │
                           │    14 tables · 4 Flyway   │
                           │    pg_trgm · uuid-ossp    │
                           └────────────┬─────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
           ┌────────▼────────┐  ┌───────▼───────┐  ┌──────▼──────┐
           │ kura-enterprise  │  │  kura-b2c-api │  │    Redis    │
           │   -api :8080     │  │     :8081     │  │  OTP/Reset/ │
           │ Auth + Orders +  │  │ Search + Share│  │  Cache      │
           │ Catalog + Results│  │ (Read-Only)   │  └─────────────┘
           └────────▲────────┘  └───────▲───────┘
                    │ ╲                 │
           ┌────────┴──╲─────┐  ┌───────┴───────┐
           │ kura-workspace  │  │  kura-b2c-web │
           │   -web :4201    │  │     :4200     │
           │ (Angular 20)    │  │ (Angular 20)  │
           │ Lab Backoffice  │  │ Patient Portal│
           └─────────────────┘  └───────────────┘

Note: kura-b2c-web calls BOTH APIs:
  → Enterprise API (8080) for auth + orders
  → B2C API (8081) for search + share
```

### 1.2 Repository Map

| Repository | Purpose | Tech | Port |
|-----------|---------|------|------|
| **`kura-enterprise-api`** | Lab admin, backoffice, B2B marketplace API. Owns Flyway migrations. | Spring Boot 4.0 / Java 25 | 8080 |
| **`kura-b2c-api`** | Patient-facing read-only API: catalog search (pg_trgm), PoS listing, share links. No auth or orders (owned by Enterprise API). | Spring Boot 4.0 / Java 25 | 8081 |
| **`kura-b2c-web`** | Patient portal: search exams, checkout, orders, share results. | Angular 20 / Tailwind 4 | 4200 |
| **`kura-workspace-web`** | Lab backoffice: dashboard, catalog CRUD, orders, results, CSV import, inventory. | Angular 20 / Tailwind 4 | 4201 |

### 1.3 Design Principles

| Principle | Implementation |
|-----------|---------------|
| **Stateless** | No JVM sessions; JWT-ready (placeholder token in MVP) |
| **Dual Backend** | Isolated Fargate scaling: B2C (high-traffic patients) vs Enterprise (lab admins) |
| **DRY/SOLID** | Interface-based providers, single-responsibility services |
| **Soft Delete** | `deleted_at` field on domain entities; queries filter `deletedAtIsNull` |
| **Audit Trail** | Immutable `audit_logs` table (Colombia compliance — 15yr retention) |
| **Tenant Isolation** | PoS-scoped queries (`WHERE pos_id = ?`) throughout |
| **Mobile-First** | Tailwind responsive design, 18px base font for older adults |

### 1.4 Clean Architecture (Both APIs)

```
co.com.kura.{enterprise|b2c}/
├── api/
│   ├── controller/         # REST Controllers
│   └── dto/                # Request/Response DTOs
├── config/                 # Security, CORS, Cache, Exception Handling
├── domain/
│   ├── entity/             # JPA Entities
│   └── repository/         # Spring Data Repositories
├── infrastructure/         # External Integration Interfaces + Mocks
└── service/                # Business Logic
```

---

## 2. Database Schema

### 2.1 Shared Database

Both APIs connect to the **same PostgreSQL database** (`kura`). Flyway migrations live exclusively in `kura-enterprise-api`. The B2C API runs with `flyway.enabled: false` and `jpa.hibernate.ddl-auto: validate`.

### 2.2 Flyway Migrations

| Version | File | Tables Created |
|---------|------|----------------|
| V1 | `V1__foundation_tables.sql` | `users`, `laboratories`, `points_of_service`, `warehouse_inventory`, `audit_logs` |
| V2 | `V2__catalog_tables.sql` | `master_services`, `bundle_items`, `lab_offerings`, `test_dependencies` |
| V3 | `V3__commerce_tables.sql` | `orders`, `order_items`, `walkin_tickets`, `payments` |
| V4 | `V4__results_and_shares.sql` | `patient_results`, `share_links` |

### 2.3 Entity Relationship Diagram

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

### 2.4 PostgreSQL Extensions

- **`uuid-ossp`** — UUID v4 primary key generation
- **`pg_trgm`** — Trigram-based fuzzy text search (indexed on `users.full_name`, `laboratories.name`, `points_of_service.name`, `master_services.name`)

### 2.5 Key Constraints

- `warehouse_inventory`: `UNIQUE(pos_id, item_code)`, `CHECK(quantity >= 0)`
- `bundle_items`: `UNIQUE(bundle_id, service_id)`, `CHECK(quantity > 0)`
- `lab_offerings`: `UNIQUE(pos_id, service_id)`
- `orders.status`: `CHECK IN ('PENDING','CONFIRMED','IN_PROGRESS','SAMPLE_TAKEN','COMPLETED','CANCELLED')`
- `orders.payment_method`: `CHECK IN ('PAY_AT_LAB','MERCADOPAGO')`
- `users.role`: `CHECK IN ('PATIENT','LAB_ADMIN','LAB_TECH','POS_MANAGER','SUPPORT','SUPER_ADMIN')`

---

## 3. Backend: kura-enterprise-api

### 3.1 Architecture

| Layer | Count | Details |
|-------|-------|---------|
| Controllers | 6 | Auth, Catalog, Commerce, Result, Import, Health |
| Services | 6 | AuthService, OtpService, CatalogService, OrderService, ResultService, PatientImportService |
| Entities | 14 | Full domain model |
| Repositories | 14 | Spring Data JPA |
| DTOs | 12 | Request + Response objects |
| Infrastructure | 8 | 4 interfaces + 4 mock implementations |
| Config | 3 | SecurityConfig, CorsConfig, GlobalExceptionHandler |

### 3.2 API Endpoints

#### Auth (`/api/v1/auth`) — AuthController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/otp/send` | Send OTP to email (Redis, 5min TTL) | Public |
| POST | `/otp/verify` | Verify OTP code | Public |
| POST | `/register` | Register patient (RNEC mock + Ley 1581) | Public |
| POST | `/login` | Login (BCrypt + HttpOnly cookie) | Public |
| POST | `/password/reset` | Request password reset (Mock SES) | Public |
| POST | `/password/reset/confirm` | Confirm reset with token | Public |

#### Catalog (`/api/v1/catalog`) — CatalogController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/services` | Create service (SINGLE/BUNDLE) | Protected |
| GET | `/services/{code}` | Get service by code | Protected |
| GET | `/services/search?q=&limit=` | pg_trgm fuzzy search | Protected |
| GET | `/services/type/{type}` | List by type | Protected |
| POST | `/offerings` | Create lab offering (PoS + price) | Protected |

#### Commerce (`/api/v1/commerce`) — CommerceController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/orders` | Create order (guest or user, single-PoS cart) | Protected |
| GET | `/orders/{orderNumber}` | Get order by number | Protected |
| GET | `/orders/user/{userId}` | User order history | Protected |

#### Results (`/api/v1/results`) — ResultController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/results/{id}/sample-taken` | Mark sample taken (triggers BOM stock deduction) | Protected |
| POST | `/results/{id}/complete` | Complete result with data + notes | Protected |
| POST | `/results/{id}/audio` | Upload audio note (Mock S3) | Protected |
| POST | `/results/{id}/share` | Create 48h share link | Protected |
| GET | `/share/{shareUuid}` | Access result via share link | **Public** |
| GET | `/results/order/{orderId}` | Results by order | Protected |
| GET | `/results/patient/{patientId}` | Results by patient | Protected |

#### Import (`/api/v1/import`) — ImportController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/patients` | CSV patient import (multipart, dynamic column mapping, PostgreSQL upsert) | Protected |

#### Health (`/api/v1`) — HealthController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/ping` | Service health check | Public |

---

## 4. Backend: kura-b2c-api

> **Scope**: Read-only catalog search and share links only. Auth and Orders are handled by the Enterprise API to avoid code duplication.

### 4.1 Architecture

| Layer | Count | Details |
|-------|-------|---------|
| Controllers | 3 | Search, Share, Health |
| Services | 2 | SearchService (JdbcTemplate + `@Cacheable`), ShareService (JdbcTemplate + JPA) |
| Entities | 6 | MasterService, LabOffering, BundleItem, PointOfService, PatientResult, ShareLink |
| Repositories | 6 | Spring Data JPA (read-only access to shared DB) |
| DTOs | 3 | SearchResponse, PosResponse, ShareResponse |
| Infrastructure | 0 | No mock providers (all integrations owned by Enterprise API) |
| Config | 4 | SecurityConfig, CorsConfig, CacheConfig, GlobalExceptionHandler |

### 4.2 API Endpoints

#### Search (`/api/v1/search`) — SearchController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/services?q=&posId=&limit=` | pg_trgm fuzzy search with Redis `@Cacheable` | Public |
| GET | `/services/{code}` | Get service detail by code | Public |
| GET | `/pos` | List active Points of Service (`@Cacheable`) | Public |

#### Share (`/api/v1/share`) — ShareController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/{shareUuid}` | Access result via 48h share link (increments access count) | Public |

#### Health (`/api/v1`) — HealthController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/ping` | Service health check | Public |

### 4.3 B2C-Specific Features

- **Redis Caching** (`@EnableCaching` + `RedisCacheManager`): search results cached 15min, PoS locations cached indefinitely via `@Cacheable`
- **No PasswordEncoder bean** — authentication is not handled here
- **Flyway disabled**: B2C does not run migrations; it validates against existing schema
- **JdbcTemplate for search**: Uses raw SQL with `pg_trgm` `similarity()` for ranked fuzzy search
- **ShareService uses JDBC for user lookup**: Patient name fetched via `JdbcTemplate` query (User entity is not mapped in B2C)
- **Runs on port 8081** to coexist with enterprise-api on 8080

---

## 5. Frontend: kura-b2c-web

### 5.1 Architecture

| Concern | Implementation |
|---------|---------------|
| Framework | Angular 20 (standalone components) |
| Styling | Tailwind CSS 4 + KURA design language |
| i18n | `@ngx-translate/core` with `provideTranslateHttpLoader` (ES/EN) |
| State | Angular signals (`signal()`, `computed()`) |
| HTTP | `HttpClient` with `withCredentials: true` interceptor |
| Routing | Lazy-loaded standalone components |
| Cart | `CartService` — localStorage-persisted, single-PoS restriction |
| Auth | `AuthService` — localStorage-persisted user, computed signals |

### 5.2 Pages & Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | `HomeComponent` | Hero, featured services, how-it-works, trust indicators |
| `/search` | `SearchComponent` | pg_trgm search, PoS selector, bundle comparison table, add-to-cart |
| `/auth/login` | `LoginComponent` | Email/password login form |
| `/auth/register` | `RegisterComponent` | 3-step flow: Email → OTP → Registration form |
| `/checkout` | `CheckoutComponent` | Cart summary, guest form, pay-at-lab flow, walk-in ticket display |
| `/orders` | `OrdersComponent` | Expandable order list with ticket codes |
| `/share/:uuid` | `ShareComponent` | Public result viewer with expiry check |

### 5.3 Services

| Service | Purpose |
|---------|---------|
| `ApiService` | HTTP client — routes to **two backends**: B2C API (search/share) + Enterprise API (auth/orders) |
| `AuthService` | Auth state management (signals, localStorage) |
| `CartService` | Shopping cart (signals, localStorage, single-PoS enforcement) |

### 5.4 Dual-API Routing

The B2C frontend calls two separate backend APIs:

| Endpoint Group | Target API | Dev URL |
|----------------|-----------|---------|
| Search, PoS listing, Share | B2C API | `http://localhost:8081/api/v1` |
| Auth (login, register, OTP, reset) | Enterprise API | `http://localhost:8080/api/v1` |
| Orders (create, list, get) | Enterprise API | `http://localhost:8080/api/v1` |

Configured via `environment.b2cApiUrl` and `environment.enterpriseApiUrl`.

### 5.5 Design Language

- **Primary**: Trust Medical Blue (`#0ea5e9`)
- **Accent**: Emerald Green (`#10b981`)
- **Typography**: 18px base (desktop), 16px (mobile) — optimized for older adults
- **Components**: Rounded cards (`rounded-2xl`), shadow hierarchy, mobile-first responsive

---

## 6. Frontend: kura-workspace-web

### 6.1 Architecture

| Concern | Implementation |
|---------|---------------|
| Framework | Angular 20 (standalone components) |
| Styling | Tailwind CSS 4 |
| i18n | `@ngx-translate/core` (ES/EN, files in `public/assets/i18n/`) |
| Layout | Sidebar navigation with collapsible mobile drawer |
| HTTP | `HttpClient` with `withCredentials: true` interceptor |

### 6.2 Pages & Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | `DashboardComponent` | KPIs (total orders, pending, active services, low inventory), quick actions, recent orders table |
| `/catalog` | `CatalogListComponent` | Service list with search + type filter (SINGLE/BUNDLE), action buttons |
| `/catalog/create` | `CatalogCreateComponent` | Create service form: SINGLE/BUNDLE toggle, bundle child picker, "Not Recommended" custom warning modal |
| `/orders` | `OrdersComponent` | Expandable orders table: items, walk-in ticket, payment status |
| `/results` | `ResultsComponent` | Results management: mark sample taken, complete with data, upload audio, create share link |
| `/import` | `ImportComponent` | CSV drag-and-drop upload with `papaparse`, visual column mapping, preview table |
| `/inventory` | `InventoryComponent` | Stock levels with OK/LOW/CRITICAL thresholds, color-coded alerts |

### 6.3 Services

| Service | Purpose |
|---------|---------|
| `ApiService` | HTTP client for enterprise API (catalog, commerce, results, import, health) |
| `LayoutComponent` | Shared sidebar + header shell |

### 6.4 Current Limitation

Most workspace pages use **hardcoded sample data** for the MVP. The `ApiService` methods are implemented and ready, but the UI components simulate API responses with `setTimeout`. Real API integration is Phase 2.

---

## 7. Security & SSO

### 7.1 Authentication Flow

1. **OTP Verification**: Email → Redis (5min TTL) → Email delivery (Mock SES)
2. **Registration**: OTP verified → RNEC identity check (mocked) → BCrypt password → User saved
3. **Login**: Email + BCrypt verify → Placeholder JWT token → `KURA_SESSION` HttpOnly cookie

### 7.2 Cookie-Based SSO

Both APIs generate the same cookie on login/register:

```
Set-Cookie: KURA_SESSION=kura-jwt-{userId};
  HttpOnly; Secure; Domain=.kura.com.co; Path=/; SameSite=Lax; Max-Age=86400
```

- Token is **removed from response body** (cookie-only transport)
- Both frontends send `withCredentials: true` on all HTTP requests

### 7.3 Spring Security Config

| API | Public Endpoints | Default |
|-----|------------------|---------|
| Enterprise | `/api/v1/auth/**`, `/actuator/health,info` | All others require authentication |
| B2C | `/api/v1/search/**`, `/api/v1/share/**`, `/api/v1/health/**`, `/actuator/health,info` | All others require authentication |

**Note**: No JWT filter is implemented yet — `authenticated()` endpoints will return 401 in MVP. This is by design; real JWT validation is Phase 2. B2C API has no auth endpoints — all authentication flows through Enterprise API.

### 7.4 CORS

Both APIs configured with `allowCredentials: true`, configurable origins via `kura.cors.allowed-origins`.

---

## 8. Colombia Compliance (Ley 1581)

| Requirement | Implementation |
|-------------|---------------|
| **Data Consent** | `consent_ley1581` + `consent_date` required at registration; blocked if `false` |
| **Identity Verification** | RNEC mock (always MATCH); `cedula` + `full_name` locked post-registration |
| **Audit Trail** | Immutable `audit_logs` table — all auth events + CSV imports logged |
| **Data Retention** | 15-year retention via soft delete + `deleted_at` (Glacier archival: Phase 2) |
| **Immutability** | Audit logs: no UPDATE/DELETE operations |

---

## 9. Key Business Logic

### 9.1 OTP Flow

1. Generate 6-digit code (SecureRandom) → Redis `otp:{email}` (5min TTL)
2. Send via Mock SES → Log to console
3. Verify: compare against Redis → Delete on success

### 9.2 Composite Catalog

- **SINGLE** services: atomic lab tests
- **BUNDLE** services: composite of SINGLEs via `bundle_items` join table
- Custom services trigger "Not Recommended" warning
- `pg_trgm` fuzzy search with `similarity()` ranking + `@Cacheable` (B2C)

### 9.3 Commerce Flow

1. Cart restricted to **single PoS** per order (frontend enforced via CartService)
2. Price lookup: PoS-specific `lab_offerings` → fallback to `base_price`
3. Walk-in ticket: `expires_at = NOW() + 15 days` (no calendar appointments)
4. MercadoPago (mocked): fake preference ID + sandbox checkout URL

### 9.4 Bundle Comparison (B2C Frontend)

- Select up to 3 bundles for side-by-side comparison
- Comparison table shows included tests with ✅ checkmarks
- Price row at bottom for quick comparison

### 9.5 Stock Deduction on SAMPLE_TAKEN

1. `markSampleTaken()` → status = `SAMPLE_TAKEN`, timestamp
2. Look up order item → get service ID
3. Query `test_dependencies` (BOM) for the service
4. Deduct `quantity_needed` from `warehouse_inventory` at the PoS
5. Log warnings for negative stock or low threshold breaches

### 9.6 Share Links

- UUID-based public URLs: `/share/{uuid}` (B2C) / `/api/v1/share/{uuid}` (API)
- 48-hour expiry enforced at access time
- Access count tracked per link
- Expired links return user-friendly error (B2C shows dedicated expired page)

### 9.7 CSV Patient Import (Enterprise)

1. Upload CSV via multipart
2. Dynamic column mapping: `mapping_cedula`, `mapping_full_name`, `mapping_email`, `mapping_phone`
3. PostgreSQL native upsert: `ON CONFLICT (cedula) DO UPDATE SET full_name = EXCLUDED.full_name`
4. Returns `CsvImportResult`: total rows, imported, updated, errors with row-level detail
5. Frontend (Workspace): `papaparse` client-side parsing, visual drag-and-drop, auto-detect columns

### 9.8 Empathy Module (Results)

- Audio note upload: WebRTC `MediaRecorder` (frontend) → Mock S3 (backend saves to `/tmp/kura-storage`)
- Result data entry: free-text JSON/structured data
- Complete workflow: PENDING → SAMPLE_TAKEN → IN_PROGRESS → COMPLETED

---

## 10. Internationalization (i18n)

### 10.1 Backend i18n

| API | Files | Coverage |
|-----|-------|---------|
| Enterprise | `messages_es.properties`, `messages_en.properties` | Auth, errors, CRUD, inventory |
| B2C | `messages_es.properties`, `messages_en.properties` | Auth, commerce, share, generic errors |

### 10.2 Frontend i18n

| App | Implementation | Files |
|-----|---------------|-------|
| B2C Web | `@ngx-translate/core` + `provideTranslateHttpLoader` | `src/assets/i18n/{es,en}.json` |
| Workspace Web | `@ngx-translate/core` + `TranslateHttpLoader` | `public/assets/i18n/{es,en}.json` |

Both default to Spanish (Colombia) — `defaultLanguage: 'es'`.

**Coverage**: All user-visible strings externalized: navigation, forms, errors, buttons, labels, status badges, instructions.

---

## 11. Registry of Mocked Integrations

> ⚠️ **All entries below are placeholders.** Each must be replaced with real implementations in Phase 2.

### 11.1 Enterprise API Mocks

| # | System | Interface | Mock Class | File | Default Behavior | Phase 2 |
|---|--------|-----------|------------|------|------------------|---------|
| 1 | AWS SES (Email) | `EmailProvider` | `MockSesEmailProvider` | `infrastructure/MockSesEmailProvider.java` | Logs to console | AWS SES SDK |
| 2 | RNEC (Colombian ID) | `IdentityVerificationProvider` | `MockRnecProvider` | `infrastructure/MockRnecProvider.java` | Always returns `MATCH` | RNEC SOAP/REST |
| 3 | MercadoPago | `PaymentProvider` | `MockMercadoPagoProvider` | `infrastructure/MockMercadoPagoProvider.java` | Fake preference ID + sandbox URL | MercadoPago SDK (COP) |
| 4 | AWS S3 (Storage) | `StorageProvider` | `MockS3StorageProvider` | `infrastructure/MockS3StorageProvider.java` | Saves to `/tmp/kura-storage` | AWS S3 SDK |

### 11.2 B2C API Mocks

> **None.** All mock integrations were removed from `kura-b2c-api` during the API responsibility split. Auth, payments, email, and identity verification are exclusively owned by `kura-enterprise-api`.

### 11.3 Workspace Web Mocks

| # | Location | What's Mocked | Detail |
|---|----------|---------------|--------|
| 8 | `dashboard.component.ts` | Dashboard KPIs | Hardcoded stats array |
| 9 | `catalog-list.component.ts` | Service catalog | `sampleServices[]` array with `setTimeout` |
| 10 | `catalog-create.component.ts` | Service creation | `setTimeout` simulating API call |
| 11 | `orders.component.ts` | Order management | `sampleOrders[]` array |
| 12 | `results.component.ts` | Results management | `sampleResults[]`, mock state transitions |
| 13 | `import.component.ts` | CSV import | `setTimeout` with random import/update/error counts |
| 14 | `inventory.component.ts` | Inventory levels | `inventoryItems[]` hardcoded array |

All mocked code contains: `// MOCK INTEGRATION: [System Name] - [Description]`

---

## 12. Configuration Reference

### 12.1 Enterprise API (`application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | API port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/kura` | Shared DB |
| `spring.datasource.username` | `${DB_USERNAME:postgres}` | DB user |
| `spring.datasource.password` | `${DB_PASSWORD:mysecretpassword}` | DB password |
| `spring.data.redis.host` | `${REDIS_HOST:localhost}` | Redis |
| `spring.flyway.enabled` | `true` | Runs migrations |
| `kura.cors.allowed-origins` | `http://localhost:4200,4201` | CORS |

### 12.2 B2C API (`application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | API port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/kura` | Shared DB |
| `spring.datasource.username` | `${DB_USERNAME:kura}` | DB user |
| `spring.datasource.password` | `${DB_PASSWORD:kura}` | DB password |
| `spring.data.redis.host` | `${REDIS_HOST:localhost}` | Redis |
| `spring.flyway.enabled` | `false` | No migrations (enterprise owns schema) |
| `spring.jpa.open-in-view` | `false` | Performance: no OSIV |

### 12.3 Frontend Environments

| App | Variable | Dev URL | Prod URL |
|-----|----------|---------|----------|
| B2C Web | `b2cApiUrl` | `http://localhost:8081/api/v1` | `/b2c-api/v1` |
| B2C Web | `enterpriseApiUrl` | `http://localhost:8080/api/v1` | `/api/v1` |
| Workspace Web | `apiUrl` | `http://localhost:8080/api/v1` | `/api/v1` |

---

## 13. Project Statistics

### 13.1 Enterprise API

| Metric | Count |
|--------|-------|
| Java source files | 38 |
| JPA entities | 14 |
| Repositories | 14 |
| REST controllers | 6 |
| REST endpoints | 18 |
| DTOs | 12 |
| Services | 6 |
| Flyway migrations | 4 |
| Mock integrations | 4 |

### 13.2 B2C API

| Metric | Count |
|--------|-------|
| Java source files | 16 |
| JPA entities | 6 |
| Repositories | 6 |
| REST controllers | 3 |
| REST endpoints | 4 |
| DTOs | 3 |
| Services | 2 |
| Mock integrations | 0 |

### 13.3 B2C Web

| Metric | Count |
|--------|-------|
| Angular components | 9 (7 pages + navbar + footer) |
| Services | 3 (API, Auth, Cart) |
| Routes | 7 |
| i18n keys | ~200 (ES + EN) |

### 13.4 Workspace Web

| Metric | Count |
|--------|-------|
| Angular components | 9 (7 pages + layout + app) |
| Services | 2 (API, Auth interceptor) |
| Routes | 7 |
| i18n keys | ~150 (ES + EN) |

### 13.5 Database

| Metric | Count |
|--------|-------|
| Tables | 14 |
| Indexes | 30+ |
| Extensions | 2 (uuid-ossp, pg_trgm) |
| Constraints | 10+ CHECK constraints |

---

## 14. Known Issues & Tech Debt

### 14.1 Bugs Fixed (BUG-001, BUG-002, BUG-003)

- **BUG-001**: SSO cookie not being set with `HttpOnly; Secure; Domain=.kura.com.co`. Fixed in both APIs.
- **BUG-002**: B2C API JPA entities had column names that didn't match the shared DB schema (9 entities, 20+ column mismatches). Fixed in `fix/entity-schema-alignment` branch.
- **BUG-003**: Enterprise API `ResultService.deductInventory()` was looking up test dependencies using `orderItemId` (an OrderItem UUID) instead of the service ID. Fixed in `fix/entity-schema-alignment` branch.

### 14.2 Refactors Applied

- **API Responsibility Split**: Removed duplicated Auth and Orders code from B2C API. Enterprise API is the single owner. B2C API is now a lightweight read-only search/share service. B2C frontend calls both APIs via dual base URLs.

### 14.3 Remaining Tech Debt

| # | Issue | Severity | Location | Notes |
|---|-------|----------|----------|-------|
| 1 | No JWT filter — authenticated endpoints always 401 | Medium | Enterprise API `SecurityConfig` | Phase 2: implement JWT validation filter |
| 2 | B2C `SearchService` accepts `posId` param but doesn't filter by it in SQL | Low | `kura-b2c-api/SearchService.java` | Should JOIN `lab_offerings` by `posId` |
| 3 | Workspace `TranslateHttpLoader` factory doesn't inject `HttpClient` | Low | `kura-workspace-web/app.config.ts` | Translation loading may silently fail |
| 4 | Workspace pages use hardcoded data (14 mock points) | Expected | `kura-workspace-web/pages/*` | Wire to real enterprise API in Phase 2 |
| 5 | No tests beyond scaffold `contextLoads()` | Medium | Both APIs | Add unit + integration tests |
| 6 | B2C `api.models.ts` interfaces don't match actual API DTOs | Low | `kura-b2c-web/models/api.models.ts` | Frontend uses different shape than backend returns |
| 7 | `AppComponent` exported name vs spec import mismatch | Low | Both web apps `app.spec.ts` | Spec expects `App`, class exports `AppComponent` |
| 8 | No `@Cacheable` in enterprise-api catalog | Low | `kura-enterprise-api/CatalogService.java` | Add Redis caching like B2C has |
| 9 | Enterprise API CORS must allow B2C frontend origin | Medium | `kura-enterprise-api/CorsConfig.java` | B2C Web now calls Enterprise API directly — needs `http://localhost:4200` in allowed origins |

---

## 15. Build & Run

### 15.1 Prerequisites

- Java 25
- Node.js 24+
- PostgreSQL 16+ (with `pg_trgm` extension)
- Redis 7+

### 15.2 Database Setup

```bash
# Create database and enable extensions
psql -U postgres -c "CREATE DATABASE kura;"
psql -U postgres -d kura -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
psql -U postgres -d kura -c "CREATE EXTENSION IF NOT EXISTS \"pg_trgm\";"
```

### 15.3 Enterprise API (runs migrations)

```bash
cd kura-enterprise-api
export DB_USERNAME=postgres DB_PASSWORD=mysecretpassword
mvn clean compile
mvn spring-boot:run   # Port 8080
```

### 15.4 B2C API

```bash
cd kura-b2c-api
export DB_USERNAME=kura DB_PASSWORD=kura
mvn clean compile
mvn spring-boot:run   # Port 8081
```

### 15.5 B2C Web

```bash
cd kura-b2c-web
npm install
ng serve --port 4200
```

### 15.6 Workspace Web

```bash
cd kura-workspace-web
npm install
ng serve --port 4201
```

---

*Generated by Opus 4.6 — KURA Ecosystem Architect — 2026-02-19*
