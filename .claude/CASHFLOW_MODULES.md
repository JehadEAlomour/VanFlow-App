# CashFlow — KMP Cash Van Sales App
## Feature Modules Specification — Build Playbook
**Version:** 1.0  |  **Date:** May 2026  |  **Tenant:** Al-Madina Trading Co. (Amman, Jordan)

---

## 0. About this Document

This is the **build playbook** for CashFlow. It is **not code**. It describes the app as a set of independent **feature modules**, each one a self-contained prompt you can hand to a developer (or feed to an AI assistant) to implement that one module end-to-end.

**How to use it**
- Modules are numbered M01 → M16. Build them in the order listed in §3 (Build Phases), not in numeric order.
- Each module ends with **Acceptance Criteria** — when those are green, the module is done.
- Each module declares its **Dependencies** explicitly. Never start a module until its dependencies are complete.
- Anything not in a module is **out of scope for v1.0** and goes to the backlog.

**What this app is**
A bilingual (Arabic RTL / English LTR) Kotlin Multiplatform app for FMCG cash-van salesmen. The salesman drives a route, sells from van stock, takes returns, collects payments (cash/cheque/transfer), and reconciles at end of day. Backend is **not yet built** — every module must work fully offline with seeded demo data and be designed for later sync to a Ktor backend.

---

## 1. Tech Stack (locked)

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0+ |
| UI | Compose Multiplatform 1.7+ |
| Targets | Android 24+, iOS 14+ |
| Architecture | Clean Architecture + MVI |
| DI | Koin 4 |
| Local DB | Room 3 (KMP) + BundledSQLiteDriver |
| Networking | Ktor 3 Client (+ SSE for AI streaming) |
| Navigation | Compose Navigation (commonMain) |
| Async | Kotlin Coroutines + Flow |
| Serialization | kotlinx.serialization |
| Date/Time | kotlinx-datetime |
| Settings | multiplatform-settings |
| Charts | koalaplot-core |
| Maps | Google Maps (Android) / MapKit (iOS) via expect/actual |
| Location | FusedLocationProviderClient (Android) / CLLocationManager (iOS) |
| Logging | Kermit |

**Project structure (locked):**
```
cashflow/
  shared/
    commonMain/  → domain, data, presentation (ViewModels), DI
    androidMain/ → Android-specific actuals (DB factory, location, etc.)
    iosMain/     → iOS actuals
  androidApp/    → Android Activity + screens
  iosApp/        → SwiftUI shell + screens
```

---

## 2. Module Index

| # | Module | Phase | Status |
|---|---|---|---|
| M01 | Project Foundation | P1 | required |
| M02 | Demo Data Seeder | P1 | required |
| M03 | Authentication & Login | P1 | required |
| M04 | Home Dashboard | P2 | required |
| M05 | Route Management | P2 | required |
| M06 | Customer Directory | P2 | required |
| M07 | Customer Dashboard | P3 | required |
| M08 | Sale Voucher | P3 | required |
| M09 | Return Voucher | P3 | required |
| M10 | Request Voucher | P3 | required |
| M11 | Collection (Receipt) | P3 | required |
| M12 | Customer Report (Tabs) | P3 | required |
| M13 | AI Assistant (Demo) | P4 | required |
| M14 | Van Stock | P4 | required |
| M15 | End of Day | P4 | required |
| M16 | Location Tracking (Shift) | P5 | required |
| M17 | Sync Engine (when backend ready) | P6 | deferred |
| M18 | Real AI Gateway integration | P6 | deferred |

---

## 3. Build Phases

**P1 — Foundation (Day 1)**
- M01 Project Foundation
- M02 Demo Data Seeder
- M03 Authentication & Login

**P2 — Navigation Surface (Days 2–3)**
- M04 Home Dashboard
- M05 Route Management
- M06 Customer Directory

**P3 — Transactions (Days 4–6)**  ← the core business value
- M07 Customer Dashboard
- M08 Sale Voucher
- M09 Return Voucher
- M10 Request Voucher
- M11 Collection
- M12 Customer Report Tabs

**P4 — Smart Features (Days 7–8)**
- M13 AI Assistant
- M14 Van Stock
- M15 End of Day

**P5 — Tracking (Days 9–10)**
- M16 Location Tracking

**P6 — Backend Integration (later, when API is ready)**
- M17 Sync Engine
- M18 Real AI Gateway

**Rule:** never move to the next phase until every required module of the current phase passes its Acceptance Criteria.

---

## 4. Cross-cutting Concerns (apply to every module)

**Language & RTL**
- App supports Arabic (default) and English.
- Language toggle in user profile; saved to multiplatform-settings.
- Arabic uses RTL layout; numbers (JOD amounts, phones, IDs) stay LTR via `unicode-bidi: plaintext` (Compose: text directions handled per inline span).
- Currency: **JOD with 3 decimals** always. `123.450 د.أ` (AR) or `123.450 JOD` (EN). Never round to 2.
- Months in Arabic use **Levantine names**: نيسان، أيار، حزيران، تموز، آب، أيلول، تشرين الأول، تشرين الثاني، كانون الأول، كانون الثاني، شباط، آذار.

**Design tokens (locked)**
- Background: `#080B12` deepest, `#0D1117`, `#131920`
- Surfaces: `#1A2232`, `#1E2A3A`, `#243044`
- Text: `#EDF0FA` (high), `#7B8BAA` (mid), `#3A4460` (low)
- Accent: `#4B8FF6` blue · `#1DC97A` green · `#F5A41A` amber · `#F04F4F` red · `#9B7FEA` purple · `#22D3C2` teal
- Border: `#1E2A3A`
- Typography: Plus Jakarta Sans (Latin), IBM Plex Sans Arabic (Arabic), IBM Plex Mono (numbers/codes)
- Radii: 8 (chip), 12 (card), 16 (sheet), 20 (button)

**State management**
- Every screen has a ViewModel in `shared/commonMain/presentation/feature/{name}/`.
- State exposed as `StateFlow<XxxState>`. Events as a sealed class `XxxEvent`.
- Side effects (navigation, toasts) emitted via `SharedFlow<XxxEffect>` — never put navigation logic inside the ViewModel; emit events the screen handles.

**Offline-first rule**
- Every write goes to Room **first**. Sync to backend is a separate concern (M17).
- Every read comes from Room. Backend (when present) only refills the cache.
- No screen ever shows a blank loading spinner if a cache exists — it shows cached data and refreshes silently.

**Error handling**
- Domain errors are `sealed class CashFlowError` with bilingual messages.
- UI never shows stack traces. Translate to Arabic by default, English when language=en.

---

## 5. The Modules

---

### M01 — Project Foundation

**Purpose**
Set up the empty KMP project skeleton: Gradle, Compose Multiplatform, DI, theme, navigation host. Nothing user-visible except a placeholder home screen.

**Phase:** P1
**Dependencies:** none
**Owner:** mobile lead

**Deliverables**
1. `settings.gradle.kts` with `:shared`, `:androidApp`, `:iosApp` modules.
2. Version catalog `gradle/libs.versions.toml` containing every library locked above in §1.
3. `shared/build.gradle.kts` with `androidTarget()` + `iosX64/iosArm64/iosSimulatorArm64()`, Compose MP, Room, Koin, Ktor.
4. `androidApp/build.gradle.kts` with minSdk 24, targetSdk 35, Compose, splash screen API.
5. Empty Compose Navigation graph wired in `MainActivity`.
6. Theme module (`shared/commonMain/presentation/theme/`) exposing `CashFlowTheme {}`, `CashFlowColors` object, `formatJod()` extension, `AppLanguage` enum.
7. Koin DI bootstrap — empty modules ready to receive registrations from M02 onward.
8. Folder structure created under `shared/commonMain/kotlin/com/cashflow/`:
   `domain/{model,repository,usecase}/`, `data/{local/{db,entity,dao},remote,seeder,repository}/`, `presentation/{feature,components,theme,navigation}/`, `di/`.

**Acceptance Criteria**
- [ ] `./gradlew :androidApp:assembleDebug` succeeds.
- [ ] App installs and shows a blank dark-themed screen with the CashFlow logo.
- [ ] Theme colours & typography render correctly on a sample text block.
- [ ] Koin starts without errors.
- [ ] An iOS framework can be produced via `./gradlew :shared:linkPodReleaseFrameworkIosArm64`.

**Out of scope**
SQLCipher encryption (added later in hardening), proguard rules, CI/CD pipeline.

---

### M02 — Demo Data Seeder

**Purpose**
Provide realistic seeded data so the app is fully usable before the backend exists. The seeder runs once on first launch, populates Room, and never runs again unless the user resets the app from settings.

**Phase:** P1
**Dependencies:** M01
**Owner:** mobile lead

**Deliverables**

1. Room database `CashFlowDatabase` with 9 entities:
   - `users` · `customers` · `products` · `invoices` · `payments` · `location_points` · `shifts` · `ai_messages` · `route_stops`
2. Schemas exported to `shared/schemas/`.
3. `DemoSeeder` singleton with one `suspend fun seed(...)` method.
4. A `Settings` flag `demo_seeded=true` so seeding only runs once.
5. Repositories' DI registrations (interfaces in M07-M11 will use them).

**Seeded data shape**

**Users (4):**
| ID | Name (AR) | Phone | Pass | Role |
|---|---|---|---|---|
| USR-001 | أحمد المصري | 0791234567 | 1234 | SALESMAN |
| USR-002 | محمد الخالد | 0799876543 | 1234 | SALESMAN |
| USR-003 | فيصل النمر | 0795551234 | 1234 | SUPERVISOR |
| USR-004 | سارة الأحمد | 0797778899 | 1234 | SALESMAN |

Passwords stored as `"demo_hash_$plain"` (replace with real Argon2/BCrypt when backend lands).

**Customers (15+):** 10 on today's route (`isOnRoute=true`, ordered by `visitOrder`), 5 off-route. Spread across realistic Amman neighbourhoods: Downtown, Jabal Amman, Sweifieh, Sahab, Zarqa, Al-Rabiyeh, Jubeiha, 7th Circle, Shmeisani, Gardens. Mix tiers A/B/C and segments CHAMPIONS/LOYAL/AT_RISK/PROMISING/DORMANT/REGULAR. At least 3 customers have overdue balances; at least 1 has `churnRisk ≥ 0.65` so the danger UI is visible immediately.

**Products (30):** spread across categories Beverages, Dairy, Snacks, Confectionery, Cleaning, Canned, Dry Goods, Water, Personal Care, Hot Beverages, Household, Cooking Oils, Special Items. At least 3 products with `vanStock < minStock` so low-stock badges appear. Realistic brands for the Jordan market: Tropicana, Pepsi, Unifresh, Lays, Pringles, Twix, Ariel, Vim, Heinz, Maggi, Nescafe, Lipton, Baraka, Head & Shoulders, Dove.

**Invoices (8):**
- 3 sale invoices today (different customers, different totals, different payment methods)
- 1 return today
- 3 historical sales (1, 2, 3 days ago)
- 1 request/pre-order today

**Payments (4):**
- 1 cash today
- 1 cheque yesterday (with bank + number + date)
- 1 bank transfer
- 1 BOUNCED cheque (for the "bounced" badge demo)

**Shifts:** 1 ACTIVE shift for USR-001 starting at 08:00 today.

**Acceptance Criteria**
- [ ] First launch: seeder runs, sets the flag, populates all 9 tables.
- [ ] Second launch: seeder does not run (verified by log + row counts).
- [ ] Calling `customerDao().observeRouteCustomers()` returns 10 customers in correct visit order.
- [ ] All money fields stored with 3-decimal precision.
- [ ] Settings reset → flag cleared → next launch re-seeds.

**Out of scope**
Pulling demo data from a JSON file (it's a hardcoded list to avoid asset bundling complexity in KMP).

---

### M03 — Authentication & Login

**Purpose**
A bilingual login screen that takes phone + password, captures the device location, and authenticates against seeded users. On success, persist a session and route to Home.

**Phase:** P1
**Dependencies:** M01, M02

**User Flow**

1. User opens the app.
2. If `current_user_id` is in Settings → skip login, go to Home.
3. Else show **Login Screen**:
   - Logo + app name (bilingual)
   - Phone field (numeric keyboard, format `07XXXXXXXX`)
   - Password field (masked, toggle visibility)
   - "Login / تسجيل الدخول" button
   - Demo credentials hint card
4. On tap "Login":
   - Request `ACCESS_FINE_LOCATION` permission.
   - If granted: read last location via FusedLocationProvider (Android) / CLLocationManager (iOS).
   - If denied: proceed with `lat=0.0, lng=0.0` and a logged warning. Do not block login on missing location.
5. Call `LoginUseCase(phone, password, lat, lng)`.
6. On success:
   - Update user's `lastLoginAt/Lat/Lng/token` in Room.
   - Save `current_user_id` and `current_token` to Settings.
   - Navigate to Home, pop the login destination off the back stack.
7. On failure: show inline error in Arabic + English (e.g. "رقم الهاتف غير مسجل / Phone not registered" or "كلمة المرور خاطئة / Wrong password").

**Models needed**
- Domain: `User`, `UserRole`
- DTO/Entity: `UserEntity`
- Error: `AuthError.UserNotFound`, `AuthError.WrongPassword`, `AuthError.LocationDenied` (warning only)

**Use cases**
- `LoginUseCase(phone, password, lat, lng) → Result<User>`
- `GetCurrentUserUseCase() → User?`
- `LogoutUseCase()`

**Validation rules**
- Phone must be 10 digits, start with `07`.
- Password min 4 characters (demo). Real rule: 8+ alphanumeric, enforced at backend later.
- Trim whitespace on phone before lookup.

**Screen layout**

```
[ Status bar — translucent ]
[ Vertical gradient bg: deep blue → black → deep blue ]

  ┌─ Logo (gradient circle 88dp, "CF" mono white) ─┐
                  كاش فلو
        CashFlow — نظام مندوب الفان

  ─────────────────────────────────────────────

  رقم الهاتف / Phone Number
  ┌──────────────────────────────────────────┐
  │ 📞  07XXXXXXXX                            │
  └──────────────────────────────────────────┘

  كلمة المرور / Password
  ┌──────────────────────────────────────────┐
  │ 🔒  ••••••••                    👁          │
  └──────────────────────────────────────────┘

  [ Error chip if any — red bg, 12% opacity ]

  ┌──────────────────────────────────────────┐
  │       ⇨  تسجيل الدخول / Login            │  ← blue, 54dp
  └──────────────────────────────────────────┘

  ┌── Demo creds card ──────────────────────────┐
  │ 🔑 بيانات تجريبية / Demo Credentials        │
  │ أحمد المصري           0791234567 / 1234     │
  │ محمد الخالد           0799876543 / 1234     │
  └─────────────────────────────────────────────┘

         Al-Madina Trading Co. © 2026
```

**Acceptance Criteria**
- [ ] App opens to login when no session exists.
- [ ] App opens to Home when a valid session exists.
- [ ] Logging in with `0791234567 / 1234` succeeds, captures location (if granted), and navigates to Home.
- [ ] Wrong password shows the bilingual error and clears it when the user types again.
- [ ] Denying location does not block login; a warning is logged.
- [ ] Logout (from Home) clears the session and returns to login.
- [ ] Password visibility toggle works.
- [ ] Back button on the login screen does nothing (already root).

**Out of scope**
Biometric login, magic link, multi-tenant workspace selector — those go to a v1.1 backlog.

---

### M04 — Home Dashboard

**Purpose**
The salesman's daily command centre. Shows today's KPIs, action tiles, and the first 5 customers in today's route.

**Phase:** P2
**Dependencies:** M01, M02, M03

**User Flow**
1. Land on Home after login.
2. See greeting with user name + today's date (Levantine month).
3. Glance at 4 KPI cards: Sales · Collections · Returns · Customers visited/planned.
4. Tap any of 5 action tiles:
   - **مسار اليوم** (Route) → M05
   - **قائمة العملاء** (Customers) → M06
   - **مخزون الفان** (Van Stock) → M14
   - **المساعد** (AI Assistant) → M13
   - **نهاية اليوم** (End of Day) → M15
5. Scroll down: top 5 customers in today's route with tier badge + overdue/churn warnings.
6. Tap any customer → M07.
7. Top-right corner: AI sparkle ✨ button → M13 ; Logout icon → M03 login.

**State (HomeViewModel)**
- `user: User?`
- `kpi: DailyKpi?` (computed: salesTotal, returnsTotal, collectionsTotal, customersVisited, customersPlanned)
- `routeCustomers: List<Customer>` (first 5 only on Home; full list on M05)
- `isLoading: Boolean`

**Computations**
- `salesTotal` = SUM of all `invoices.total` where `type=SALE` AND `status≠CANCELLED` AND `createdAt ≥ today 00:00`.
- `returnsTotal` = same as above but `type=RETURN`.
- `collectionsTotal` = SUM of `payments.amount` where `status=CONFIRMED` AND `createdAt ≥ today 00:00`.
- `customersVisited` = count of distinct `customerId` from today's invoices (i.e. anyone we made a transaction with).
- `customersPlanned` = count of customers where `isOnRoute=true`.

**Acceptance Criteria**
- [ ] Greeting shows the logged-in user's Arabic name.
- [ ] Date displays in Levantine Arabic (e.g. "الخميس 15 أيار").
- [ ] All 4 KPIs render with correct values from seeded data.
- [ ] All 5 action tiles navigate to the correct destination.
- [ ] Top-5 customer rows show tier badge, area, overdue warning if any, churn risk warning if ≥ 0.60.
- [ ] "عرض الكل" button under the route navigates to M05.
- [ ] Logout button returns to login and clears the session.
- [ ] Pull-to-refresh reloads KPIs.

**Out of scope**
Animated chart on Home (deferred to v1.1). Notifications bell.

---

### M05 — Route Management

**Purpose**
Show today's full route — every customer the salesman is supposed to visit, in visit order, with a progress bar showing how many they've already transacted with. Searchable.

**Phase:** P2
**Dependencies:** M01, M02, M03, M04

**User Flow**
1. From Home, tap "مسار اليوم" or "عرض الكل".
2. See: header with count, progress bar (visited / planned), search box, scrollable list of customer cards in visit order.
3. Search input filters by name (AR/EN), customer code, or area. Debounced 300ms.
4. Each card shows: visit-order badge, customer name (AR), area, tier pill, and warning chips (overdue / churn).
5. Tap any card → M07 Customer Dashboard for that customer.
6. Header has a "الكل / المسار فقط" toggle to switch between route-only and full directory (which is actually M06).

**State (RouteViewModel)**
- `routeCustomers: List<Customer>` (only those with `isOnRoute=true`, ordered by `visitOrder`)
- `searchQuery: String`
- `searchResults: List<Customer>`
- `isSearching: Boolean`
- `visitedCount: Int` (count of distinct customerIds from today's invoices)
- `isLoading: Boolean`

**Acceptance Criteria**
- [ ] Shows exactly the customers seeded with `isOnRoute=true`, in correct visit order.
- [ ] Progress bar reflects today's visits.
- [ ] Search is debounced and case-insensitive, works in both AR and EN.
- [ ] Clearing the search shows the full route again.
- [ ] Tapping a customer opens M07 for that customer.
- [ ] Overdue chip appears only when `overdueAmount > 0`.
- [ ] Critical churn (≥0.80) gives the card a faint red wash.

**Out of scope**
Map view with pins (handled in M16). Drag-reorder of route (server-side optimisation, deferred to v1.1).

---

### M06 — Customer Directory

**Purpose**
The full customer book — every customer, not just today's route — with search and filters by tier/segment.

**Phase:** P2
**Dependencies:** M01, M02, M03

**User Flow**
1. From Home → "قائمة العملاء".
2. See: header, search box, filter chips row (الكل · فئة A · فئة B · فئة C), scrollable customer list.
3. Search: name AR/EN, code, area. Debounced.
4. Filters are toggleable (tap again to clear). Combine with search.
5. Tap any customer → M07.
6. Each card shows: avatar circle with first AR letter, name AR + customer code + area, tier pill, segment chip (with churn-coloured background), balance amount (if > 0), "خارج المسار" pill if `!isOnRoute`.

**State (CustomerListViewModel)**
- `allCustomers: List<Customer>`
- `searchQuery: String`
- `searchResults: List<Customer>`
- `tierFilter: CustomerTier?`
- `segmentFilter: CustomerSegment?`
- `isLoading: Boolean`

**Acceptance Criteria**
- [ ] Shows all 15+ seeded customers regardless of `isOnRoute`.
- [ ] Filter chips work additively with search.
- [ ] Segment chip colour: green ≤30%, amber 30–65%, red ≥65% churn.
- [ ] Off-route customers have the "خارج المسار" pill.
- [ ] Sort order: route customers first (by `visitOrder`), then off-route alphabetically by `nameAr`.

**Out of scope**
Creating a new customer from the app (v1.1 — needs backend support for customer codes).

---

### M07 — Customer Dashboard

**Purpose**
The central screen of the app. Everything about one customer in one place: profile, financial health, transaction history, and a bottom action bar to start any voucher type.

**Phase:** P3
**Dependencies:** M01, M02, M05 or M06 (entry point)

**User Flow**
1. Land here from M04, M05, or M06 with a `customerId` arg.
2. Top: customer info card — name, code, phone, address, tier badge, financials (balance/overdue/credit limit), churn risk warning if applicable.
3. Below: 3 KPI chips — total sales to this customer, total returns, total collected.
4. Tabs (scrollable row): **الملخص / المبيعات / المرتجعات / الطلبات / التحصيلات**.
5. Tab body shows the relevant list, sorted newest first.
6. Bottom action bar (always visible): **بيع / مرتجع / طلب / تحصيل** — each tile coloured and tappable.
7. Top-right: AI sparkle button → M13 with `customerId` in the route, so the assistant has context.

**Tab contents**

| Tab | Shows | Empty state text |
|---|---|---|
| الملخص | Summary card: invoice count, payment count, balance, overdue, credit limit, tax number | — |
| المبيعات | All `SALE` invoices for this customer | "لا توجد فواتير بيع" |
| المرتجعات | All `RETURN` invoices | "لا توجد مرتجعات" |
| الطلبات | All `REQUEST` invoices (pre-orders) | "لا توجد طلبات" |
| التحصيلات | All `Payment` records (cash/cheque/transfer) | "لا توجد تحصيلات" |

Each invoice row: type badge (بيع/مرتجع/طلب coloured), invoice number, datetime, line count, total, payment method.
Each payment row: receipt number, method, datetime, amount, cheque info if applicable, status (مؤكد / مرتجع / معلق).

**State (CustomerDashboardViewModel)**
- `customer: Customer?` (observed flow — auto-refreshes when balance changes)
- `invoices: List<Invoice>` (all types for this customer)
- `payments: List<Payment>`
- `salesTotal/returnsTotal/collectionsTotal: Double`
- `selectedTab: CustomerTab`
- `isLoading: Boolean`

**Constructor parameter:** `customerId: String` — passed via navigation arg, injected through Koin `parametersOf`.

**Bottom Action Bar — navigation targets**
- **بيع** → M08 SaleVoucherScreen(customerId)
- **مرتجع** → M09 ReturnVoucherScreen(customerId)
- **طلب** → M10 RequestVoucherScreen(customerId)
- **تحصيل** → M11 CollectionScreen(customerId)

**Acceptance Criteria**
- [ ] Renders within 200ms from a cached customer.
- [ ] Customer info, financials, and churn warning render correctly.
- [ ] All 5 tabs work; switching is smooth (no flicker).
- [ ] Each tab's empty state shows the correct Arabic message.
- [ ] Invoice rows display correct type colours: SALE green, RETURN red, REQUEST teal.
- [ ] Payment rows show cheque number + bank for cheques, hide that line for cash/transfer.
- [ ] Bounced cheque payments are tagged "مرتجع" in red.
- [ ] Each bottom-bar tile navigates to the correct voucher screen.
- [ ] After saving any voucher in M08–M11, returning to this screen shows the new transaction immediately.
- [ ] AI button passes `customerId` so M13 can use customer context.

**Out of scope**
Customer statement PDF export (v1.1). Customer photo upload.

---

### M08 — Sale Voucher

**Purpose**
The salesman's primary action: sell products from van stock to the current customer, with optional discount, applied tax, payment method, and notes.

**Phase:** P3
**Dependencies:** M01, M02, M07

**User Flow**

1. From Customer Dashboard, tap **بيع** → SaleVoucherScreen opens with the customer context.
2. Two sub-views toggleable from the top-right cart icon:
   - **Product Picker** (default): search bar + product list grouped by category. Tap a product to open a quantity sheet (or +1 / -1 inline). Stock badge shows on each product card.
   - **Cart View**: shows added lines with editable quantity, line discount, and an X to remove. Includes a notes field.
3. As lines are added, the bottom totals strip updates live:
   ```
   المجموع الفرعي :  XX.XXX د.أ
   الخصم          : -X.XXX د.أ
   الضريبة (16%) :  X.XXX د.أ
   ─────────────────────────────
   الإجمالي       :  XX.XXX د.أ   ← bold green
   ```
4. Tap **حفظ الفاتورة** → modal sheet appears:
   - Payment method radio: **نقداً / آجل / شيك / تحويل**
   - If cheque/transfer: extra fields (cheque number, bank, date) — optional at sale-time, can be added later via M11.
   - Confirm button.
5. On confirm:
   - Validate stock: every line's `qty ≤ product.vanStock`. If any line fails, show inline error "الكمية غير متوفرة في الفان لـ {product}".
   - Compute totals (server-side rules — see below).
   - Persist `Invoice(type=SALE, status=CONFIRMED)` to Room.
   - Reduce van stock for each line.
   - If payment method is `CREDIT` → increase customer balance by `total`.
   - Pop back to Customer Dashboard with a snackbar "✓ تم حفظ الفاتورة {number}".

**Business rules**
- Tax: 16% VAT, applied to (subtotal − discount).
- Discount: either a single overall discount amount (JOD) or per-line discount percentage. Both supported; if both used, overall discount applies after line discounts.
- Stock: hard-blocked on insufficient stock. Show the available number.
- Invoice number format: `INV-YYYYMMDD-####` where `####` is the day-sequence padded to 4 digits.
- All money fields stored at 3 decimals.
- Lines serialised as JSON in `invoices.linesJson` (avoids a separate `invoice_lines` table for now).

**State (SaleVoucherViewModel)**
- `customer: Customer?`
- `products: List<Product>`
- `cart: List<CartLine>` (productId, qty, unitPrice, discountPct)
- `searchQuery: String`, `searchResults: List<Product>`
- `discountAmount: Double` (overall)
- `selectedPaymentMethod: PaymentMethod`
- `notes: String`
- `isSaving: Boolean`, `savedInvoice: Invoice?`, `error: String?`

**Computed properties:** `subtotal`, `taxAmount`, `total`. Live updated.

**Use case**
`CreateSaleVoucherUseCase(customer, salesmanId, lines, discountAmount, paymentMethod, notes) → Result<Invoice>`

Performs: stock validation → compute totals → persist invoice → adjust stock → adjust balance.

**Acceptance Criteria**
- [ ] Saving an invoice writes a row to `invoices` with correct totals.
- [ ] `products.vanStock` decreases by the right amount for each line.
- [ ] `customers.balance` increases only when payment method is CREDIT.
- [ ] Stock < qty blocks save and shows a clear bilingual error.
- [ ] Returning to Customer Dashboard shows the new invoice in the المبيعات tab.
- [ ] Customer balance KPI updates in real time (StateFlow).
- [ ] Snackbar confirms the new invoice number.
- [ ] Cart can be cleared.
- [ ] Notes field is optional; empty notes → null in DB.

**Out of scope**
Signature capture (will be needed when printing or for credit sales — moved to v1.1). Barcode scanning to add products.

---

### M09 — Return Voucher

**Purpose**
Customer returns goods. Salesman picks the products, quantity, and reason; the system creates a RETURN invoice that reduces the customer balance and adds stock back to the van.

**Phase:** P3
**Dependencies:** M01, M02, M07

**User Flow**
1. From Customer Dashboard → **مرتجع**.
2. Same UI pattern as Sale (product picker + cart) but:
   - No payment method picker.
   - A "سبب الإرجاع" (return reason) field is shown — required, with quick-pick chips: **منتهي الصلاحية / تالف / خطأ في الطلب / آخر**.
3. Save → creates `Invoice(type=RETURN, status=CONFIRMED)`, increases stock for each line, **reduces** customer balance by `total`.
4. Pop back, snackbar "✓ تم تسجيل المرتجع {number}".

**Business rules**
- Tax still applied at 16% (returns are tax-inclusive).
- Stock increases (van receives the returned items).
- Balance decreases (customer is credited).
- Reason is stored in the `notes` field as `سبب: {reason} — {extra notes}`.
- Number format: `RET-YYYYMMDD-####`.

**State (ReturnVoucherViewModel)**
Same shape as Sale but no payment method, no overall discount, mandatory reason.

**Acceptance Criteria**
- [ ] Saving a return writes a `type=RETURN` invoice.
- [ ] Stock for each line increases by the returned qty.
- [ ] Customer balance decreases by the return total.
- [ ] Return appears in Customer Dashboard's المرتجعات tab.
- [ ] Empty reason blocks save.

**Out of scope**
Linking to the original sale invoice (v1.1; needs cross-reference UI).

---

### M10 — Request Voucher

**Purpose**
A pre-order: customer wants items the van doesn't have today. Salesman captures the order; it will be fulfilled on the next visit. Does not affect van stock or balance immediately — it's a commitment, not a sale.

**Phase:** P3
**Dependencies:** M01, M02, M07

**User Flow**
1. Customer Dashboard → **طلب**.
2. Product picker + cart (same UI pattern).
3. Optional expected-delivery date field.
4. Notes field.
5. Save → creates `Invoice(type=REQUEST, status=CONFIRMED)`. **No** stock change, **no** balance change.

**Business rules**
- Just a record. Number format `REQ-YYYYMMDD-####`.
- Status flow: CONFIRMED → (later) FULFILLED → converted to a SALE invoice. (Fulfilment is v1.1.)

**Acceptance Criteria**
- [ ] Saving creates a `type=REQUEST` invoice.
- [ ] Stock and balance are unchanged.
- [ ] Appears under الطلبات tab on Customer Dashboard.
- [ ] Lines can include products that are out of stock in the van (since it's a pre-order).

**Out of scope**
Converting a REQUEST to a SALE (v1.1). Managing all open requests in one place (v1.1).

---

### M11 — Collection (Receipt of Payment)

**Purpose**
Record money received from a customer toward their balance. Supports cash, cheque, and bank transfer. Reduces customer balance immediately.

**Phase:** P3
**Dependencies:** M01, M02, M07

**User Flow**
1. Customer Dashboard → **تحصيل**.
2. Form fields (vertical):
   - Customer (read-only, shows name + current balance)
   - Amount (numeric, JOD with 3 decimals)
   - Method (segmented control: **نقداً / شيك / تحويل**)
   - If method == شيك → reveal: cheque number, bank name (dropdown of common Jordan banks: Arab Bank, Bank of Jordan, Jordan Islamic Bank, Cairo Amman Bank, Housing Bank, ABC Bank, others...), cheque date (picker)
   - If method == تحويل → reveal: transfer reference number
   - Notes (optional)
3. Save → creates a `Payment(status=CONFIRMED)`, reduces `customer.balance` by amount.
4. Pop back, snackbar "✓ تم تسجيل تحصيل {number}".

**Validation**
- Amount > 0.
- Amount can exceed current balance (advance payment) — show a warning but allow.
- Cheque method requires cheque number AND bank (date is optional, defaults to today).
- Transfer method requires reference number.

**Business rules**
- Receipt number format: `RCP-YYYYMMDD-####`.
- Cheque date stored as epoch millis at start-of-day in user TZ.
- Status defaults to CONFIRMED. Later (manager action) it can be flipped to BOUNCED, which then **adds back** to customer balance (handled in M17 sync engine, not here).

**State (CollectionViewModel)**
- `customer: Customer?`
- `amount: String` (kept as string for keyboard input; parsed at save)
- `method: PaymentMethod`
- `chequeNumber/chequeBank/chequeDate/transferRef: String/Long`
- `notes: String`
- `isSaving: Boolean`, `savedPayment: Payment?`, `error: String?`

**Acceptance Criteria**
- [ ] Saving a cash collection writes a `Payment` row and reduces `customer.balance`.
- [ ] Method switch shows/hides cheque fields correctly.
- [ ] Cheque without number or bank blocks save.
- [ ] Amount > balance shows a warning chip but doesn't block save.
- [ ] Collection appears in Customer Dashboard التحصيلات tab.
- [ ] Customer balance in the header updates immediately.

**Out of scope**
OCR cheque scanning (deferred to AI phase). Multiple cheques in one collection (v1.1).

---

### M12 — Customer Report Tabs (already wired in M07)

**Purpose**
Confirm the spec for the tabs inside Customer Dashboard. (This module is not a separate screen — it's the contract for the 5 tabs in M07.)

**Phase:** P3
**Dependencies:** M07, M08, M09, M10, M11

**Tabs and their data sources**

| Tab | Source query | Sort |
|---|---|---|
| الملخص | computed counts from invoices/payments | — |
| المبيعات | `invoices WHERE customerId=X AND type='SALE'` | createdAt DESC |
| المرتجعات | `invoices WHERE customerId=X AND type='RETURN'` | createdAt DESC |
| الطلبات | `invoices WHERE customerId=X AND type='REQUEST'` | createdAt DESC |
| التحصيلات | `payments WHERE customerId=X` | createdAt DESC |

All as `Flow<List<...>>` so they auto-refresh when M08–M11 write new rows.

**Acceptance Criteria**
- [ ] After saving in M08, the new invoice appears in المبيعات tab within 500ms.
- [ ] Same for M09 → المرتجعات, M10 → الطلبات, M11 → التحصيلات.
- [ ] Switching tabs does not refetch; each tab observes a hot flow.

---

### M13 — AI Assistant (Demo Mode)

**Purpose**
A chat surface the salesman can open from anywhere to ask quick questions. Since the AI Gateway backend doesn't exist yet, this module ships with **demo responses** that match common keyword queries. Designed so the real API (M18) can drop in without UI changes.

**Phase:** P4
**Dependencies:** M01, M02, M03

**User Flow**

1. Two entry points:
   - Floating ✨ button on Home top bar (no customer context).
   - ✨ button on Customer Dashboard top bar (passes `customerId` so the assistant has context).
2. Screen opens with a welcome message:
   > أهلاً! أنا مساعدك الذكي لتطبيق كاش فلو. يمكنني مساعدتك في: ملخص اليوم، تحليل أداء العملاء، توصيات المنتجات، استفسارات المخزون...
3. 6 quick-action chips above the input (تmappable):
   **ملخص اليوم · أعلى عميل · ذمم متأخرة · توقع المبيعات · آخر فاتورة · أداء الأسبوع**
4. User types or taps a chip → message appears with "thinking..." dots → after 1.2s simulated latency, AI response renders.
5. Long messages render Markdown (lists, bold, emojis allowed).
6. No streaming yet (M18 will add SSE).

**Demo response table (keyword → response)**

| Keyword (any of) | Response |
|---|---|
| `ملخص`, `summary` | Today's KPI summary in Arabic markdown |
| `مبيعات`, `sales` | Bulleted list of today's sale invoices with totals |
| `عميل`, `customer` | If `customerId` is set: that customer's analysis (tier, last visit, balance, churn). If not: list top customers. |
| `مخزون`, `stock` | List of low-stock products with quantities |
| `مسار`, `route` | Today's remaining customers with warnings |
| Anything else | Polite fallback: "🤔 سؤال رائع! في الوقت الحالي أعمل بالبيانات التجريبية. جرب: ملخص، مبيعات، مخزون، مسار، عميل" |

**State (AiAssistantViewModel)**
- `messages: List<AiMessage>` (starts with welcome message)
- `inputText: String`
- `isThinking: Boolean`
- `customerId: String?` (constructor arg)
- `isOffline: Boolean` (always true in demo mode)

**Persistence**
- Each message saved to `ai_messages` Room table with a generated `conversationId`.
- History retained 30 days, then auto-deleted (cron is M17).
- For now: every Home-entry chat is one conversation; every customer-context chat is its own conversation keyed by `customerId`.

**Acceptance Criteria**
- [ ] Welcome message renders immediately on open.
- [ ] Tapping a quick-action chip sends that text.
- [ ] Each keyword in the table returns the correct response.
- [ ] Unknown queries return the fallback message.
- [ ] "Thinking..." dots animate for 1.2s before the response appears.
- [ ] Messages persist across navigation away and back.
- [ ] Customer-context entry produces a different conversation than Home entry.
- [ ] AI messages have the ✨ badge to distinguish from user messages.

**Out of scope**
Real LLM calls, SSE streaming, voice input — all deferred to M18.

---

### M14 — Van Stock

**Purpose**
Show the salesman the inventory currently in their van: which products, how many, low-stock warnings, expiry warnings.

**Phase:** P4
**Dependencies:** M01, M02, M03

**User Flow**
1. From Home → **مخزون الفان**.
2. See: header with total SKU count and total inventory value, search box, scrollable product list grouped by category.
3. Each product card shows: name AR + EN, SKU, current van stock, min stock threshold, status pill (good / low / out / expiring), sale price.
4. Low stock (< min) → amber badge "⚠️ منخفض" and the card stays at top of its category.
5. Out of stock → red badge "نفد".
6. Expiring within 30 days → amber "⏰ ينتهي قريباً".
7. Tap a product → simple detail sheet showing all fields + a "طلب تجديد" button (which creates a REQUEST voucher addressed to the warehouse, deferred to v1.1).

**State (VanStockViewModel)**
- `products: List<Product>`
- `lowStockProducts: List<Product>`
- `searchQuery: String`
- `selectedCategory: String?`
- `totalInventoryValue: Double` (sum of `vanStock × salePrice`)

**Acceptance Criteria**
- [ ] All 30 seeded products appear.
- [ ] Products with `vanStock < minStock` are visibly tagged.
- [ ] Stock decreases in real-time when M08 saves a sale.
- [ ] Stock increases in real-time when M09 saves a return.
- [ ] Search works across name AR/EN, SKU, category.

**Out of scope**
Warehouse restock request workflow (v1.1).

---

### M15 — End of Day

**Purpose**
Daily wrap-up screen the salesman opens before going home. Shows the day's totals, cash to reconcile, and a "End Shift" button that closes the shift.

**Phase:** P4
**Dependencies:** M01, M02, M03, M11, M16 (shift exists)

**User Flow**
1. From Home → **نهاية اليوم**.
2. See:
   - Big summary card: total sales, total returns, net sales, total collections.
   - Cash reconciliation card: total cash collected today (sum of payments where method=CASH). The salesman must confirm this matches the physical cash in their pocket.
   - Customers visited / planned ratio.
   - List of unsynced invoices and payments (with a "Sync now" button if online — M17).
3. Button: **إنهاء اليوم وتسجيل الخروج** at the bottom.
4. On tap:
   - Confirmation dialog.
   - If confirmed: end the active shift in the `shifts` table (set `endedAt` and `status=ENDED`).
   - Logout → back to login screen.

**State (EndOfDayViewModel)**
- `kpi: DailyKpi`
- `cashCollectedToday: Double`
- `chequesCollectedToday: Double`
- `transfersCollectedToday: Double`
- `unsyncedInvoices: Int`, `unsyncedPayments: Int`
- `activeShift: Shift?`

**Acceptance Criteria**
- [ ] All daily totals are correct.
- [ ] Cash collected = sum of `method=CASH` payments today.
- [ ] End-shift button is disabled if there are unsynced items AND backend is required (in demo mode: always enabled).
- [ ] After ending shift, `shifts.status=ENDED` and the user is logged out.

**Out of scope**
Generating a printable end-of-day report (v1.1, needs M17 + a PDF skill).

---

### M16 — Location Tracking & Shift Lifecycle

**Purpose**
Track the salesman's GPS position throughout an active shift. Buffer locally; sync later. Detect stops and off-route deviations on-device.

**Phase:** P5
**Dependencies:** M01, M02, M03, M15

**Key components**
1. `LocationTracker` (expect/actual) — wraps FusedLocationProviderClient on Android and CLLocationManager on iOS.
2. `AndroidForegroundService` — keeps tracking alive when the app is in the background. Persistent notification: "كاش فلو يتابع الموقع — الوردية نشطة".
3. `StopDetector` — pure Kotlin Haversine engine. Fires a `StopEvent` if the salesman stays within 50m for ≥ 3 minutes.
4. `RouteValidator` — geofence check against planned route waypoints. Fires `OffRouteEvent` when more than 300m from any route segment.
5. `LocationRepository.savePoint(...)` — always writes to Room first, then optionally pushes via WebSocket.

**Shift lifecycle**
- Shift starts implicitly on first login of the day (M03 wires this) OR explicitly via a "بدء اليوم" button on Home if no active shift exists.
- Shift ends via M15.
- During an active shift: GPS interval 5s when moving, 30s when idle. Min distance filter 10m.

**Battery budget:** ≤ 8% over an 8-hour shift.

**Permissions**
- Android: `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` + `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION`.
- iOS Info.plist: `NSLocationWhenInUseUsageDescription`, `NSLocationAlwaysAndWhenInUseUsageDescription`, `UIBackgroundModes: location`.

**Acceptance Criteria**
- [ ] Starting a shift requests background-location permission and begins emitting points.
- [ ] Every emitted point is persisted to `location_points` with `synced=false`.
- [ ] Foreground service notification appears and cannot be dismissed.
- [ ] Stopping the shift cancels the service and clears the notification.
- [ ] Standing still for 3 minutes produces a `StopEvent`.
- [ ] Battery use over an 8-hour test: ≤ 8%.
- [ ] Killing the app does not lose buffered points; relaunching shows them all.

**Out of scope**
Live WebSocket streaming to the server (needs backend → M17). OSRM map snapping (server-side, M17). The AI shift report (M18).

---

### M17 — Sync Engine (when backend is ready)

**Purpose**
Push every locally-created invoice, payment, location point, and shift to the backend; pull updates back. Resilient to flaky networks; idempotent; never loses local writes.

**Phase:** P6 (deferred)
**Dependencies:** all M01–M16; backend Ktor endpoints

**Strategy**
- Outgoing queue: `WHERE syncedAt IS NULL AND status IN ('CONFIRMED', 'BOUNCED')`.
- Batch size: 50 records per HTTP call.
- Retry: exponential backoff (2^n seconds, capped at 30s, infinite retries during shift).
- Conflict resolution: server wins on customer master data; client wins on invoices/payments it created.
- WebSocket for live location streaming (best-effort, fire and forget; HTTP batch is the source of truth).

**Endpoints expected**
- `POST /api/v1/invoices` — body: list of unsynced invoices.
- `POST /api/v1/payments` — body: list of unsynced payments.
- `POST /api/v1/tracking/batch` — body: list of location points.
- `WS  /api/v1/tracking/live` — live position stream.
- `POST /api/v1/shifts/start` and `/end`.
- `GET  /api/v1/customers` — diff sync via `updatedSince` query param.
- `GET  /api/v1/products` — same pattern.

**Out of scope until P6.**

---

### M18 — Real AI Gateway

**Purpose**
Replace the demo responses in M13 with real Claude-powered streaming. Plus all the other AI features the spec calls out: recommendations, churn, segmentation, cheque OCR, voice order, anomaly check, route optimisation, briefings, coaching.

**Phase:** P6
**Dependencies:** M13 (UI), M17 (network)

**Out of scope until P6.** The contract is already defined by the PROMPT_2 source document; that becomes the build spec for this phase.

---

## 6. Module Connection Map

```
                            ┌──── M03 Login ────┐
                            │                    │
                            ▼                    │
                    ┌── M04 Home ──┐             │
                    │              │             │
        ┌───────────┼──────┬───────┼─────────────┘
        │           │      │       │
        ▼           ▼      ▼       ▼
   M05 Route   M06 Cust.  M14 Stock  M15 End-of-Day
        │       Dir.                 │
        └───┬───┘                    ▼
            │                   M16 Tracking (shift)
            ▼
       M07 Customer Dashboard
        │   │   │   │   │   ╲
        │   │   │   │   │    ╲
        ▼   ▼   ▼   ▼   ▼     ▼
       M08 M09 M10 M11 M12  M13 AI
       │    │   │   │   (tabs)
       └────┴───┴───┴────────────► writes back to Room
                                  → auto-refreshes M04, M07, M14

       Every write also flagged for M17 Sync Engine
       (synced=false until M17 acks)
```

---

## 7. Definition of Done (for v1.0)

The app is "done" when all of these are true:

1. Every required module (M01–M16) passes its Acceptance Criteria.
2. App runs on Android 7.0+ and iOS 14.0+.
3. Bilingual UI works correctly in both AR and EN; RTL/LTR layouts are correct on both platforms.
4. Demo data seeds on first run; subsequent runs preserve user data.
5. A salesman can log in, do a full day's work (route through 10 customers, with sales/returns/requests/collections at each), and end the day — all fully offline.
6. The DB never loses a write, even with airplane mode + force-kill mid-transaction.
7. Battery use is < 8% over an 8-hour shift.
8. App launches to first frame in < 1.5 seconds on a mid-range device.
9. No crashes in a 30-minute smoke test covering every screen.

---

## 8. Out of Scope for v1.0

(Tracked in a v1.1 backlog file, not built unless asked.)

- Customer creation from the app
- Signature capture
- PDF export of invoices / statements / EoD reports
- Barcode scanning
- Biometric login
- Multi-tenant workspace selector
- Notifications (push + local)
- Manager dashboard (separate web app)
- Drag-reorder of route
- Cheque OCR / Voice order / Real AI streaming
- Map view with live pins (just route list for now)
- SQLCipher encryption (kept plain Room for v1.0 to keep iOS build simple)
- Multi-language beyond AR/EN

---

## 9. How to Use This Doc with an AI Assistant

For each module, prompt like this:

> "Build module **M07 Customer Dashboard** per the spec in CASHFLOW_MODULES.md. The dependencies (M01, M02) are already in place. Produce: the ViewModel in `shared/commonMain/presentation/feature/customer/`, the Compose screen in `androidApp/.../screens/CustomerDashboardScreen.kt`, the navigation entry in the existing nav graph, and the Koin viewModel registration. Honour the bilingual rules and design tokens in §4. Stop after Acceptance Criteria are met."

That gives the assistant: scope, dependencies, deliverables, contract, and stop condition. No guessing.

---

*End of CashFlow Module Spec v1.0.*
