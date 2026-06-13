# FlowVan — Build Progress

Tracks every completed phase. Updated at phase sign-off (BUILD SUCCESSFUL verified).

---

## ✅ P1 — Foundation (Complete)

**Modules:** M01 Project Foundation · M02 Demo Data Seeder · M03 Authentication & Login

**Deliverables**
- `:shared` + `:composeApp` KMP module structure
- `gradle/libs.versions.toml` — full version catalog (Room 2.7.0, Koin 4.0.0, KSP 2.3.7, kotlinx-datetime 0.6.2, Compose MP 1.10.3)
- `shared/build.gradle.kts` — Room plugin + KSP for Android/iosArm64/iosSimulatorArm64
- `FlowVanDatabase` — 9 entities: User, Customer, Product, Invoice, Payment, LocationPoint, Shift, AiMessage, RouteStop
- `DatabaseFactory` (expect/actual) + `BundledSQLiteDriver` (no `Dispatchers.IO` on iOS)
- `DemoSeeder` — 4 users, 15 customers (10 on-route), 30 products (3 low-stock), 8 invoices, 4 payments
- `LoginUseCase` / `LogoutUseCase` / `GetCurrentUserUseCase`
- `SessionStore` (multiplatform-settings)
- `SharedModule` (Koin) + `platformModule` (Android/iOS expect/actual)
- `FlowVanNavHost` skeleton
- Dark theme tokens (`Fv` object): BgDeepest `#080B12`, 6 accents, surface layers
- `formatJod()` — JOD 3-decimal formatter
- `formatLevantine()` — Levantine Arabic month names

**Key fixes applied**
- `Dispatchers.IO` removed from `DatabaseProvider` (not available on K/N)
- `kotlin.time.Clock` used instead of `kotlinx.datetime.Clock.System` (iOS compat)
- `navigation-compose` kept only in `:composeApp` (iOS klib cache failure with alpha13)

**Build sign-off:** `:shared:linkDebugFrameworkIosSimulatorArm64` + `:composeApp:compileDebugKotlinAndroid` → `BUILD SUCCESSFUL`

---

## ✅ P2 — Navigation Surface (Complete)

**Modules:** M04 Home Dashboard · M05 Route Management · M06 Customer Directory

**Deliverables**
- `HomeScreen` + `HomeViewModel` — daily KPI strip, top route customers list, quick-action cards (Route/Stock/AI/EndOfDay), logout
- `RouteScreen` + `RouteViewModel` — ordered route customer list with overdue/churn warnings, visited toggle
- `CustomerListScreen` + `CustomerListViewModel` — full customer list with search, tier badges, overdue indicators
- `GetDailyKpiUseCase` — live aggregation from Room (sales/returns/collections/visited ratio)
- All routes wired: `home`, `route`, `customers`
- `ComingSoonScreen` placeholder for P4+ screens (VAN_STOCK, AI, END_OF_DAY)

**Build sign-off:** `BUILD SUCCESSFUL`

---

## ✅ P3 — Transactions (Complete)

**Modules:** M07 Customer Dashboard · M08 Sale Voucher · M09 Return Voucher · M10 Request Voucher · M11 Collection · M12 Customer Report Tabs

**Deliverables**
- `CustomerDashboardScreen` + `CustomerDashboardViewModel` — 5-tab layout (ملخص / مبيعات / مرتجعات / طلبات / تحصيلات), bottom action bar (بيع / مرتجع / طلب / تحصيل), header with balance/churn/tier
- `SaleVoucherScreen` + `SaleVoucherViewModel` — product picker → cart toggle, discount field, 16% VAT, payment method dialog (CASH/CREDIT/CHEQUE/TRANSFER), stock validation
- `ReturnVoucherScreen` + `ReturnVoucherViewModel` — reason chip selector (DAMAGED/EXPIRED/WRONG_ITEM/CUSTOMER_REFUSAL), confirmation dialog, stock restored on save
- `RequestVoucherScreen` + `RequestVoucherViewModel` — pre-order flow, no stock check
- `CollectionScreen` + `CollectionViewModel` — method picker (CASH/CHEQUE/TRANSFER), conditional cheque fields + Jordan banks LazyRow, advance-payment warning
- Shared UI components: `ProductPickerColumn`, `CartLineRow`, `TotalsStrip`, `QtyStepper`
- Use cases: `CreateSaleVoucherUseCase`, `CreateReturnVoucherUseCase`, `CreateRequestVoucherUseCase`, `RecordCollectionUseCase`, `VoucherNumber`
- All parameterized ViewModels via `koinViewModel { parametersOf(customerId) }`
- Routes: `customer/{id}`, `sale/{id}`, `return/{id}`, `request/{id}`, `collection/{id}`

**Build sign-off:** `BUILD SUCCESSFUL` (17s)

---

## ✅ P4 — Smart Features (Complete)

**Modules:** M13 AI Assistant · M14 Van Stock · M15 End of Day

**Build sign-off:** ✅ `BUILD SUCCESSFUL` (2m 25s)

---

## ✅ P4-ext — M19 Map Navigation (Complete)

**Modules:** M19 In-App Map Navigation

**Deliverables**
- `MapNavigationViewModel` — fetches customer lat/lng + device last known location via existing `LocationProvider`
- `PlatformMapContent` (expect/actual) — Android: `maps-compose 6.4.1` GoogleMap with customer marker + blue geodesic polyline; iOS: UIKitView + MKMapView with annotation + `showsUserLocation`
- `MapNavigationScreen` — header, full-screen map, bottom info card with "🚗 ابدأ الملاحة" button opening Google Maps via `LocalUriHandler`
- 🚗 button added to `CustomerListScreen` and `RouteScreen` cards (only when `customer.lat != null`)
- Route: `map/{customerId}` added to NavHost
- Google Maps API key added to AndroidManifest `<meta-data>`
- `maps-compose 6.4.1` + `play-services-maps 19.0.0` added to `libs.versions.toml` + `composeApp/build.gradle.kts`
- M19 module spec added to CASHFLOW_MODULES.md

**Build sign-off:** ✅ `BUILD SUCCESSFUL` (30s)

---

## ✅ P4-ext2 — M12-ext Customer Reports (Complete)

**Modules:** M12 extension — full customer reporting suite + dashboard simplification

**Deliverables**

### DAO layer
- `InvoiceDao`: added `observeById(id)`, `observeByCustomerRange(from, to)`, `observeByCustomerTypeRange(type, from, to)`
- `PaymentDao`: added `observeById(id)`, `observeByCustomerRange(from, to)`, `observeByCustomerMethodRange(method, from, to)`

### ViewModels (all in `:shared`, registered in `SharedModule`)
- `TransactionReportViewModel(customerId)` — `flatMapLatest` on (typeFilter, from, to); `TxnTypeFilter` enum (ALL/SALE/RETURN/REQUEST)
- `PaymentReportViewModel(customerId)` — `flatMapLatest` on (methodFilter, from, to); `PaymentMethodFilter` enum (ALL/CASH/CHEQUE/TRANSFER)
- `AccountStatementViewModel(customerId)` — `combine(invoiceFlow, paymentFlow)` → merged `List<StatementEntry>` sorted DESC
- `VoucherDetailViewModel(invoiceId)` — `observeById` + `json.decodeFromString<List<InvoiceLine>>(linesJson)`
- `ReceiptDetailViewModel(paymentId)` — `observeById`
- `VoucherReportViewModel(customerId)` — `flatMapLatest` on (typeFilter, kindFilter, from, to); kind applied in-memory via `.applyKind()`

### Screens (all in `:composeApp`)
- `ReportComponents.kt` — shared composables: `DateRangeBar` (Material3 DatePickerDialog), `FilterChipRow<T>`, `SummaryPill`, `Long.toDateString()`, `Long.toDateTimeString()`
- `TransactionReportScreen` — date range + type filter + sales/returns summary pills + invoice list
- `PaymentReportScreen` — date range + method filter + total/confirmed pills + payment list (rows clickable → ReceiptDetail)
- `AccountStatementScreen` — date range + debits/credits/net summary + chronological entries (invoices + payments), rows clickable → VoucherDetail or ReceiptDetail
- `VoucherDetailScreen` — type/status badges, line items table, totals block (subtotal → discount → tax → total)
- `ReceiptDetailScreen` — large amount display, method/status badges, conditional cheque card, conditional transfer card, notes card
- `VoucherReportScreen` — two filter rows (type + kind), count/total summary card, clickable VoucherRow → VoucherDetail

### CustomerDashboardScreen rewrite
- **Removed**: TabRow, 5-tab layout, inline InvoiceRow/PaymentRow composables
- **Added**: `SummaryCard` (always visible above report buttons), `ReportButtons` (3 cards: VoucherReport / PaymentReport / AccountStatement)
- Net parameter change: removed `onOpenTransactionReport`, kept `onOpenVoucherReport` + `onOpenPaymentReport` + `onOpenAccountStatement`

### Routes added to NavHost
`voucherreport/{customerId}`, `payreport/{customerId}`, `statement/{customerId}`, `txnreport/{customerId}`, `voucher/{invoiceId}`, `receipt/{paymentId}`

### Key fixes
- `kotlin.time.Instant` ≠ `kotlinx.datetime.Instant`: used `Clock.System.now().toEpochMilliseconds()` then `Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)` in all 3 date-range ViewModels
- Cross-module smart cast: extracted `val transferRef = entity.transferRef` / `val notes = entity.notes` before null checks in `ReceiptDetailScreen`
- Directions API `X-Android-Cert` fix: changed from uppercase-colon format to lowercase-no-colon format in `PlatformMapContent.android.kt`

**Build sign-off:** ✅ `BUILD SUCCESSFUL`

---

## ✅ P5 — Tracking (Complete)

**Modules:** M16 Location Tracking & Shift Lifecycle

**Deliverables**
- `LocationTracker` interface (commonMain) — `isTracking`, `locationUpdates: SharedFlow<LatLng>`, `startTracking()`, `stopTracking()`
- `AndroidLocationTracker` (androidMain/shared) — FusedLocationProviderClient with 5s interval, 10m displacement, `tryEmit` to SharedFlow. Starts/stops `TrackingForegroundService` via string class name (avoids cross-module compile dep).
- `IosLocationTracker` (iosMain/shared) — polls `IosLocationProvider.lastLocation()` every 5s in a coroutine loop
- `TrackingForegroundService` (composeApp/androidMain) — `startForeground` with `FOREGROUND_SERVICE_TYPE_LOCATION`, persistent Arabic notification "كاش فلو يتابع الموقع — الوردية نشطة", `START_STICKY`
- `StopDetector` (commonMain) — Haversine engine, fires stop event when within 50m for ≥3 min (once per stop)
- `LocationRepository` (commonMain) — `savePoint()` inserts `LocationPointEntity(synced=false)` to Room
- `StartShiftUseCase` (commonMain) — finds existing active shift or creates a new one (`SHF-{millis}`)
- `LocationTrackingCoordinator` (commonMain) — singleton with own `CoroutineScope`, collects `locationUpdates` → `LocationRepository.savePoint()` + `StopDetector.process()`
- `ShiftDao.observeActive(userId): Flow<ShiftEntity?>` — new reactive query
- `HomeViewModel` updated — observes active shift, auto-starts/stops coordinator, handles `HomeEvent.StartShift`
- `HomeState` updated — `activeShift: ShiftEntity?`
- `HomeScreen` — `ShiftStatusCard`: shows green "الوردية نشطة" chip when active, or clickable "بدء اليوم" card when inactive
- `AndroidManifest.xml` — added `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` permissions + service declaration with `foregroundServiceType="location"`
- `PlatformModule.android.kt` — registers `LocationTracker` → `AndroidLocationTracker`
- `PlatformModule.ios.kt` — registers `LocationTracker` → `IosLocationTracker`
- `SharedModule` — registers `LocationRepository`, `StopDetector`, `LocationTrackingCoordinator`, `StartShiftUseCase`

**Build sign-off:** ✅ `BUILD SUCCESSFUL` (assembleDebug)

---

---

## ✅ P6 — Backend Integration + Reports (Complete)

**Modules:** M17 Sync Engine · M18 Real AI Gateway · M20 Salesman Reports

### M18 — Real AI Gateway
- `AiSettings` — stores Claude API key in Settings (persisted locally)
- `FlowVanHttpClient` (expect/actual) — OkHttp (Android) / Darwin (iOS) Ktor engines
- `ClaudeApiClient` — manual SSE streaming via `preparePost + bodyAsChannel + readUTF8Line`
- `AiAssistantState` updated — `isStreaming`, `streamingContent`, `apiKeySet`, `showApiKeyDialog`
- `AiAssistantViewModel` — streams tokens when key set, falls back to demo mode when blank
- `AiAssistantScreen` — streaming bubble with `▌` cursor, ⚙ API key dialog, online/offline status

### M17 — Sync Engine
- `SyncConfig` — backend URL in Settings (empty = disabled, sync is a no-op)
- `SyncApi` — Ktor POST to `/api/invoices/batch`, `/api/payments/batch`, `/api/tracking/batch`
- `SyncRepository` — batches 50 unsynced invoices + 50 payments + 100 location points
- `SyncScheduler` — 60s interval coroutine loop, `start()`/`stop()` lifecycle
- `HomeViewModel` — starts/stops `SyncScheduler` with the active shift

### M20 — Salesman Reports
- **Reports Hub** — 📊 tile on HomeScreen, 5 report cards + 4 coming-soon suggestions
- **AllSalesReportScreen** — date range + type filter (مبيعات/مرتجعات/طلبات/الكل), totals pills, clickable invoice rows → VoucherDetail
- **AllPaymentsReportScreen** — date range + method filter (نقد/شيك/حوالة), per-method totals, clickable rows → ReceiptDetail
- **VisitReportScreen** — route customers with ✓/○ visit indicator, visit-rate progress bar, per-customer sales total
- **CashFlowReportScreen** (الكشف اليومي) — merged invoices + payments sorted DESC, net cash summary, clickable entries
- **ItemsSalesReportScreen** (مبيعات الأصناف) — parses linesJson from SALE invoices, aggregates by product, ranked by revenue with share bar

**Suggested reports added to hub (قادمة):**
- تقرير الطلبات · تقرير الذمم المتأخرة · تقرير المرتجعات · تقرير أداء العملاء

**Build sign-off:** ✅ `BUILD SUCCESSFUL` (assembleDebug)

---

## ✅ P7 — VanFlow Backend API Integration (Complete)

**Goal:** wire the live VanFlow NestJS backend (`.claude/FLOW-API.md`) into the offline-first
app. Plan: `.claude/API-INTEGRATION-PLAN.md`. Stays offline-first — network refills Room, UI
reads Room. Money mapped fils→JOD at the mapper boundary.

### Networking foundation (`data/remote/network/`)
- `ApiConfig` — base URL in Settings (`API_BASE_URL`), `isEnabled`, `urlFor(path)`
- `ApiEnvelope<T>` + `OffsetPage<T>` + `KeysetPage<T>` + `ApiErrorEnvelope`
- `Money.kt` — `Int/Long.filsToJod()`, `Double.jodToFils()`, `String.numericStringToDouble()`
- `FlowVanApiClient` — bearer-auth, envelope unwrap, reified `getData/postData/postEmpty/patchData/deleteUnit`; HTTP errors → `CashFlowError.Network.*`
- `CashFlowError.Network` family added (NotConfigured/Unreachable/Unauthorized/Forbidden/NotFound/Server/Validation)
- Dedicated request-encoding `Json` (`explicitNulls=false`, `encodeDefaults=false`) so optional fields aren't sent as `null`

### DTOs + mappers (`data/remote/dto/`, `data/remote/mapper/`)
- Auth, Customer, Product, Invoice, Collection, Rep DTOs + request bodies
- `RemoteMappers.kt` — DTO → domain `User` / `CustomerEntity` / `ProductEntity` / `Invoice` / `Payment` / `DailyKpi` (role + status + fils→JOD mapping)

### API services (`data/remote/api/`)
- `AuthApi` (login, me) · `CustomerApi` (list, getById, create, logVisit) · `ProductApi` (list, getById, quote) · `InvoiceApi` (list, getById, returnable, create, confirm) · `CollectionApi` (list, summary, create, confirm) · `RepApi` (kpis, vanStock, postLocation)

### Repositories + use cases
- `CustomerRepository.cacheAll()`, `ProductRepository.cacheAll()`, `UserRepository.cache()` — offline-first cache refill
- `BackendLoginUseCase` (userNumber/password, persists JWT + caches user)
- `RefreshCatalogUseCase` (customers + products in parallel)
- `SubmitInvoiceUseCase` (create + confirm), `SubmitCollectionUseCase`

### Wiring
- All registered in `sharedModule()` (single APIs + factory use cases)
- `SettingsViewModel` + `SettingsScreen` — "خادم النظام" card: API base URL field + "تحديث العملاء والأصناف من الخادم" button calling `RefreshCatalogUseCase`

**Deferred (foundation makes each a one-file add):** Vouchers, Cheques, Credit Notes, Routes,
Regions, Tax, Audit, Notification Rules, JoFotara, WebSocket realtime.

**Build sign-off:** ✅ `:shared:compileDebugKotlinAndroid` + `:shared:compileKotlinIosSimulatorArm64` + `:composeApp:compileDebugKotlinAndroid` → `BUILD SUCCESSFUL`

### P7.1 — Live validation against running backend
Tested against a live VanFlow tunnel (`/docs-json`, 116 paths). Verified via curl: health,
`auth/login` (admin/admin1234), `customers`, `products`, invoice **create+confirm**, collection create.
Findings folded in:
- **`repId` ≠ user `id`** — login payload carries a separate `repId` (and customers have their own).
  Invoice/collection endpoints require `repId`. Now captured at login → `SessionStore.currentRepId`
  (new `CURRENT_REP_ID` key); `ApiUserDto`/`MeDto` gained `repId`.
- Invoice line `quantity` comes back as a **quoted number** (`"2.000"`) — relies on the client's
  lenient Json to coerce to Double. Covered by `RemoteDtoTest`.
- `ApiConfig.DEFAULT_BASE_URL` pre-filled to the dev tunnel so the app talks to the backend out of the box.
- `LoginViewModel` now backend-aware: when `ApiConfig.isEnabled` it logs in by `userNumber` via
  `BackendLoginUseCase` (input filter relaxed to alphanumeric); offline → demo phone login.
- `RemoteDtoTest` (4 tests, commonTest) decodes captured login/customers/products/invoice payloads
  through the real DTOs+mappers → **all pass** (`:shared:testDebugUnitTest`).

### P7.2 — Removed demo data (backend is now source of truth)
- Deleted `DemoSeeder.kt` (4 users / 15 customers / 30 products / units / invoices / payments / shift) and its DI + seed call.
- `seeder.seedIfNeeded()` in `FlowVanNavHost` replaced with `PurgeDemoDataUseCase()` — a one-time
  local wipe (guarded by `DEMO_PURGED`) of previously-seeded tables so existing installs come up clean;
  app settings / AI messages / route stops / location points are preserved.
- Added `deleteAll()` to 7 DAOs (users, customers, products, product_units, invoices, payments, shifts).
- Removed the demo-credentials card from `LoginScreen`.
- Catalog now populated via `RefreshCatalogUseCase` from the backend; sales/collections via the API.

**Build sign-off:** ✅ `:shared` (Android + iOS) + `:composeApp` (Android) → `BUILD SUCCESSFUL`

---

## ✅ P8 — Offline-first sync (write-through + auto-refresh)

**Goal:** always refresh data from the API on login/home, write transactions through to the
server while saving locally, flag anything not yet uploaded, and retry on reconnect.

### Read path — auto-refresh
- `HomeViewModel` runs `RefreshCatalogUseCase` on init and on every Home (re)entry — non-blocking
  (local data shows immediately, KPIs recompute after the pull). Covers "on login" (login → Home)
  and "return to home".

### Write path — write-through + flag
- Each create use case (`CreateSaleVoucherUseCase`, `CreateReturnVoucherUseCase`,
  `CreateRequestVoucherUseCase`, `RecordCollectionUseCase`) saves locally with `syncedAt = null`
  then calls `SyncScheduler.syncNow()` — immediate push, but the row stays flagged if offline.
- `SyncRepository` rewritten to push via the **real VanFlow API** (replaced the placeholder
  `SyncApi` batch endpoints, now deleted): SALE invoices → `InvoiceApi.create + confirm`,
  collections → `CollectionApi.create`, GPS trail → `RepApi.postLocationBulk`. Uses
  `session.currentRepId`; money JOD→fils via `LocalSyncMappers`. Per-record try/catch — a failure
  leaves `syncedAt = null` for the next attempt. RETURN/REQUEST vouchers stay flagged (no backend
  endpoint yet — logged, never lost).

### Retry on reconnect
- `ConnectivityObserver` (expect/actual) — Android `ConnectivityManager.registerDefaultNetworkCallback`
  emits on `onAvailable`; iOS stub (relies on poll). Added `ACCESS_NETWORK_STATE` permission.
- `SyncScheduler` now: 60s poll **+** reconnect trigger **+** `syncNow()` one-shot. Started in
  `HomeViewModel.init` regardless of shift (decoupled from the shift lifecycle).

**Build sign-off:** ✅ `:shared` (Android + iOS) + `:composeApp` (Android) → `BUILD SUCCESSFUL`

---

## ✅ P9 — Van stock server sync

The `/products` list carries no per-rep quantity, so after the demo removal van stock was always 0.
Verified live that confirming a sale does **not** auto-decrement server van stock → must push explicitly.

- **Pull (server-first, on login/home):** `RefreshCatalogUseCase` refreshes products, then overlays
  `GET /reps/{repId}/van-stock` via `ProductRepository.setStock(id, qty)` (`RepApi.vanStock` + new
  `ProductDao.setStock`). Runs after the products upsert (which zeroes stock).
- **Push (on every voucher):** `VanStockSyncer` (fire-and-forget) — sale → `RepApi.returnVanStock`
  (subtract), customer-return → `RepApi.loadVanStock` (add). Called from the sale/return use cases
  after local `adjustStock`. Local + server both updated; offline pushes reconcile on next pull.
- Verified live: `van-stock/return` 100→98, `van-stock/load` 98→103.

**Build sign-off:** ✅ `:shared` (Android + iOS) + `:composeApp` (Android) → `BUILD SUCCESSFUL`

---

## ✅ P10 — Vouchers: all three types via POST /vouchers

Replaced the per-type push (SALE→/invoices, RETURN/ORDER skipped) with the **unified
`POST /vouchers`** endpoint using `transKind`:
- SALE → `SALE`, RETURN → `RETURN`, REQUEST → `ORDER`.
- Server had `SALE` but not `RETURN`/`ORDER` → `SyncRepository.ensureVoucherKinds()` creates them once
  via `POST /vouchers/kinds` (SALE -1, RETURN +1, ORDER 0). Verified live.
- `VoucherApi.create` + `InvoiceEntity.toVoucherRequest(userCode, customerNumber, json)`: builds
  `transactions` (itemNumber=sku, qty/unitPrice as 3-dec strings via `toAmountString`, tax/discount via
  `toPercentString`) and a CASH/CREDIT `payment` for SALE; `isPosted=true` posts on create.
  `voucherNumber = local id` → a retried push hits `409` which is mapped to `CashFlowError.Network.Conflict`
  and treated as already-synced (no duplicates).
- Needs `userCode` → added `SessionStore.currentUserCode` (`= user.userNumber`, set at backend login) and
  `customerNumber` looked up from `CustomerDao.findById(customerId).code`.
- **All three voucher types now upload** (previously RETURN/REQUEST were local-only). Collections still
  post to `/collections`; van-stock push unchanged.

**Build sign-off:** ✅ `:shared` (Android + iOS) + `:composeApp` (Android) → `BUILD SUCCESSFUL`

---

## ✅ F12 — Salesman GPS Tracking (field-quality + status UX)

Spec: `.claude/F12-mobile-salesman-tracking.md`. The streaming pipeline (FusedLocation tracker →
Room `location_points` queue → `SyncRepository` bulk drain via `RepApi.postLocationBulk` → backend)
already existed from P5/P8 — including START_STICKY foreground service, 100 m accuracy gate, device
capture-time `recordedAt`, ≤500-point batches, delete-only-after-201, central 401 → `signalUnauthorized`
with the queue kept. F12 closed the remaining field-quality + UX gaps:

- **Queue cap (§4):** `LocationPointDao.trimQueueToCap(cap)` keeps the newest 5000 *pending* pings,
  dropping the oldest beyond it; called from `LocationRepository.savePoint()` after each insert.
- **Stationary de-dupe (§4):** `LocationTrackingCoordinator` now tracks the last-enqueued point and
  skips a fix that is < 10 m from it *and* < 60 s newer (in-memory haversine), reset on `stop()`.
- **Sync status surface (§7, AC2):** `LocationPointDao.observeUnsyncedCount(): Flow<Int>` +
  `SyncScheduler.lastSyncAt: StateFlow<Long?>` (stamped on each successful non-skipped sync). Surfaced
  in `HomeState` (`pendingPings`, `lastSyncAt`) and rendered on the active-shift hero card as a
  `تمت المزامنة` / `N بانتظار المزامنة` chip plus `آخر مزامنة HH:mm` (new strings, AR + EN).

**Build sign-off:** ✅ `:composeApp:compileDebugKotlinAndroid` + `:shared:compileKotlinIosSimulatorArm64` → `BUILD SUCCESSFUL`
