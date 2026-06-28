# FlowVan — Backend API Integration Plan

Wires the live **VanFlow** NestJS backend (`.claude/FLOW-API.md`) into the existing
offline-first KMP app. The app stays offline-first: **network refills Room, the UI keeps
reading Room.** Nothing reads the network directly from a composable.

Base URL: `http://<host>:3000/api/v1`. Every response is the success envelope
`{ success, data, timestamp }`; money in invoices/collections/products/KPIs is integer **fils**
(1 JOD = 1000 fils) and must be divided to the `Double` JOD the domain models use.

All new code lives under `shared/.../data/remote/`.

---

## Phase 0 — Networking foundation  ✅ implement first

`data/remote/network/`
- `ApiConfig` — base URL from `Settings` (`API_BASE_URL`), `isEnabled`.
- `ApiEnvelope<T>` (`success`, `data`, `timestamp`) + `OffsetPage<T>` (`items`,`total`) + `KeysetPage<T>` (`items`,`total`,`page`,`limit`,`pages`).
- `ApiErrorEnvelope` (`statusCode`,`message`,`error`,`path`,`timestamp`).
- `Money.kt` — `Int.filsToJod()`, `Double.jodToFils()`.
- `CashFlowError.Network` family added to the existing error sealed class
  (Unreachable, Unauthorized, Forbidden, NotFound, Server, Validation, NotConfigured).
- `FlowVanApiClient` — wraps `HttpClient` + `ApiConfig` + `SessionStore`. Reified
  `getData/postData/patchData/deleteUnit` helpers: prepend base URL, attach
  `Authorization: Bearer <token>`, unwrap the envelope, map non-2xx → `CashFlowError.Network`.

## Phase 1 — DTOs + mappers  (`data/remote/dto/`, `data/remote/mapper/`)
- Auth: `LoginRequest`, `LoginResponseDto`, `ApiUserDto` → `User`.
- Customers: `CustomerDto` → `CustomerEntity`.
- Products: `ProductDto` → `ProductEntity` (fils→JOD).
- Invoices: `InvoiceDto`, `InvoiceLineDto`, `CreateInvoiceRequest`, `ReturnableLineDto`.
- Collections: `CollectionDto`, `CreateCollectionRequest`, `CollectionSummaryDto`.
- Reps: `RepKpiDto`, `VanStockItemDto`, `LocationPingRequest`.

## Phase 2 — API services  (`data/remote/api/`)
- `AuthApi` — `login`, `me`.
- `CustomerApi` — `list`, `getById`, `insights`, `create`, `logVisit`.
- `ProductApi` — `list`, `getById`, `quote`.
- `InvoiceApi` — `list`, `getById`, `create`, `confirm`, `returnable`.
- `CollectionApi` — `list`, `summary`, `aging`, `create`, `confirm`.
- `RepApi` — `kpis`, `vanStock`, `postLocation`.

## Phase 3 — Repository integration (offline-first refresh)
Existing repos gain suspend `refresh*()` that pull from the API and `upsertAll` into Room.
- `CustomerRepository.refreshAll()`, `ProductRepository.refreshAll()`.
- `InvoiceRepository.pushAndConfirm()` (create+confirm on backend, cache locally).
- New `AuthRepository` (backend login by `userNumber`).

## Phase 4 — Use cases
- `BackendLoginUseCase` (userNumber/password) — separate from demo `LoginUseCase`.
- `RefreshCatalogUseCase` (products + customers in parallel).
- `RefreshCustomersUseCase`, `RefreshProductsUseCase`.
- `SubmitInvoiceUseCase`, `SubmitCollectionUseCase`.

## Phase 5 — Wiring
- DI registration in `sharedModule()`.
- `ApiConfig` base-URL field in the Settings screen.
- `RefreshCatalogUseCase` triggered on Home load when `ApiConfig.isEnabled`.

---

### Deferred (foundation makes these one-file additions)
Vouchers, Cheques, Credit Notes, Routes, Regions, Tax, Audit, Notification Rules,
JoFotara, WebSocket realtime. Each is a new `XxxApi` + DTOs following the same shape.

### Conventions kept
- `kotlin.Result<T>` from use cases; `CashFlowError` in state; Arabic UI strings.
- `Clock.System.now().toEpochMilliseconds()` bridge for time (per arch-decisions).
- All money fils→JOD at the mapper boundary, never in the UI.
