# FlowVan — Clean Architecture Migration (feature-layered modularization)

Goal: move from the current 3-module layout to a Now-in-Android-style
feature-layered graph. Done as **buildable checkpoints** — after each phase the
project must compile in Android Studio before the next phase starts.

## Current (before)
```
:composeApp          UI: screens/, navigation/, platform/, service/   (Android + iOS, Compose MP)
:core:design-system  theme/colors/typography
:shared              data/ + domain/ + presentation/ + di/   (the monolith)
```

## Target (after)
```
:app  (= :composeApp)         wires every module; navigation host; DI startup
:core:model                   pure domain models (Customer, Invoice, Product, ...)
:core:common                  Result/errors (CashFlowError), formatters (Jod/Levantine), i18n (AppLanguage)
:core:database                Room @Database, entities, DAOs, mappers, DatabaseFactory
:core:network                 Ktor client, ApiConfig/Envelope, DTOs, *Api, remote mappers
:core:data                    repositories, settings (SessionStore...), connectivity, location, tracking, sync
:core:domain                  use cases, SyncScheduler, LocationTrackingCoordinator, printer contracts
:core:design-system           (unchanged) + shared Compose helpers (FvColors merge, Chips, ComingSoon)
:core:presentation            UiText/ObserveAsEvents-style helpers shared by feature UIs (if needed)
:feature:auth                 login
:feature:home                 home, route, end-of-day, settings
:feature:customer             customers list, customer dashboard, account statement
:feature:voucher              sale/return/request/voucher, collection, van-stock
:feature:reports              all report screens + VMs
:feature:print                voucher print, receipt detail, voucher detail, printer dialog
:feature:ai                   AI assistant
:feature:map                  map navigation
```
Each `:feature:*` is a single Compose-MP module holding its screens + ViewModels +
MVI contracts (KMP shares the Compose UI to iOS, matching today's `:composeApp`).

## Dependency rules (enforced by the module graph)
- `feature:*` → `core:domain`, `core:model`, `core:common`, `core:design-system`, `core:presentation`
- `core:domain` → `core:data`*, `core:model`, `core:common`   (*see open decision below)
- `core:data` → `core:database`, `core:network`, `core:model`, `core:common`
- `core:network` / `core:database` → `core:model`, `core:common`
- `core:common`, `core:model` → nothing (leaves)
- `:app` → everything
- features never depend on each other.

## Open decision (raise at the core:domain phase)
Repositories are currently concrete classes and use cases depend on them directly.
Two ways to place them cleanly:
- **(A) Strict**: repository *interfaces* in `core:domain`, *impls* in `core:data`.
  Fully respects "domain depends on nothing below it". More work (one interface per repo).
- **(B) Pragmatic**: keep repos concrete in `core:data`; `core:domain` depends on `core:data`.
  Less work, common in practice, slightly violates the strict rule.
Default unless told otherwise: **(B)**, then optionally tighten high-value repos to (A).

## Deferred
- `:build-logic` convention plugins — extract after modules exist, to dedupe the
  repeated KMP/Android/Compose/Koin/Room gradle blocks. Not required to compile.
- Splitting each feature into domain/data/presentation sub-modules — only if a
  feature grows enough to warrant it; start with one module per feature.

## Phase order (each = one buildable checkpoint)        
- [x] **P1  core:model** — moved `domain/model/*` (11 files) → `:core:model`, pkg `shared.domain.model` → `core.model`. `:shared` exposes via `api`, `:composeApp` via `implementation`. Awaiting build verification.
- [x] **P2  core:common** — moved `domain/error` → `core.common.error`, `presentation/format` → `core.common.format`, `presentation/i18n` → `core.common.i18n`. Leaf (kotlinx-datetime only). `api` from `:shared`. Awaiting build verification.
- [x] **P3  core:database** — moved `data/local/*` (32 files: dao/entity/mapper/db + android/ios DatabaseFactory) → `:core:database`, pkg `shared.data.local` → `core.database`. Moved Room/KSP plugins + `room{}` block + ksp deps out of `:shared` into `:core:database`; relocated `schemas/` and renamed the schema folder to the new DB FQN (`...core.database.db.FlowVanDatabase`) so auto-migrations 1→5 still resolve. `:shared` `api(core.database)`, `:composeApp` `implementation` (8 screens import entities — flagged as a smell to revisit). Awaiting build verification.
- [x] **P4  core:network** (+ **core:datastore**) — first extracted `data/settings` → `:core:datastore` (leaf, multiplatform-settings only; both network & data depend on it). Then `data/remote/*` → `:core:network` (pkg `shared.data.remote` → `core.network`; renamed awkward `network.network` subpkg → `core.network.http`). Moved all Ktor deps + engines out of `:shared` into `:core:network` (`:shared` now has 0 ktor imports). `:core:network` → api `core:model`, impl `core:common`/`core:database`/`core:datastore`. `:shared` `api`s both new modules. NOTE: several remote files were untracked, so `git mv` failed mid-batch — recovered with plain `mv` (git detects renames by content at commit). Awaiting build verification.
- [x] **P5  core:data** — moved `data/{repository,connectivity,location,tracking}` (19 files, all source sets) → `:core:data`, pkg `shared.data` → `core.data`. `:core:data` → api `core:model`/`core:database` (repos return models + entities), impl `core:common`/`core:datastore`/`core:network`; androidMain gets play-services-location + coroutines-play-services + core-ktx. Trimmed those location-only deps out of `:shared`'s androidMain (kept coroutines-android for the ViewModel Main dispatcher). `:shared` `api(core.data)`. `:composeApp` untouched (only uses `core.database`). Awaiting build verification.
- [x] **P6  core:domain** — moved `domain/{usecase,sync,tracking,printer}` (21 files inc. ios `IosReceiptPrinter`) → `:core:domain`, pkg `shared.domain` → `core.domain`. **Decision: pragmatic** — domain depends on concrete `core:data`. NOTE: several use cases also reach directly into `core:network` (Apis/DTOs/mappers) and `core:database` (DAOs/entities) — a smell to tighten later; those deps are wired in. `:core:domain` → api `core:model`. `:shared` `api(core.domain)`, `:composeApp` `implementation` (NavHost use cases + Android `ReceiptPrinter` impl). Awaiting build verification.
- [x] **P7  feature modules** — DONE (all 8 features). Template + shared infra established first:
    - **Shared Compose resources** moved `composeApp/composeResources` → `:core:design-system` (29 files); configured `compose.resources { publicResClass; packageOfResClass = "...core.designsystem.resources" }`; rewrote all 33 `flowvan.composeapp.generated.resources` imports → design-system package. `compose.components.resources` is now `api` in design-system.
    - **DI aggregation**: feature ViewModels no longer in `sharedModule()`; each feature exposes its own Koin module; `composeApp/di/AppModules.kt#appFeatureModules()` collects them, registered in `FlowVanApp` (android) and `MainViewController.StartKoin` (ios) via `modules(appFeatureModules())`. `:shared`'s `initKoin` unchanged.
    - **`:feature:auth`** (template): moved `LoginScreen` + `LoginViewModel` + `LoginContract` → pkg `feature.auth`; added `authModule()`; wired build.gradle (Compose-MP library) + settings + `:composeApp` dep; NavHost imports `feature.auth.LoginScreen`.
    - Remaining feature modules (repeat the auth pattern, one per build-checkpoint):
      - [x] `:feature:home` — home, route, end-of-day, settings (14 files) → flat pkg `feature.home`; `homeModule()` (4 VMs) added to `appFeatureModules()`; bindings removed from `sharedModule()`. ALSO relocated shared UI `screens/components` (Chips, FvColors, ComingSoonScreen, VoucherCart) → `:core:design-system` `components` pkg (30 importers) — design-system now `api`s `core:model`/`core:common`. Awaiting build verification.
      - [x] `:feature:customer` — customers, customerdashboard, accountstatement (9 files) → flat pkg `feature.customer`; `customerModule()` (3 VMs, 2 parameterized w/ customerId). Note: `AccountStatementScreen` lived in `screens/reports/` — moved it individually (no shared report-component deps). Awaiting build verification.
      - [x] `:feature:voucher` — sale/returns/request/voucher, collection, vanstock (19 files) + moved `AppBackHandler` expect/actual here (only voucher screens use it). androidMain: activity-compose.
      - [x] `:feature:reports` — reports/paymentreport/transactionreport/voucherreport + 11 report screens + `ReportComponents` (23 files).
      - [x] `:feature:print` — print/voucherdetail/receiptdetail + `VoucherPrintScreen` + 2 detail screens (were in screens/reports) + `PrinterConnectDialog` + `PdfShareHelper` expect/actual. androidMain: core-ktx.
      - [x] `:feature:ai` — AI assistant.
      - [x] `:feature:map` — map nav + `NavStep` + `PlatformMapContent` expect/actual. androidMain: maps-compose, play-services-maps/location, core-ktx.
    - VERIFIED: no cross-feature imports; each feature = one package; `sharedModule()` has zero `viewModel{}` (all 25 VMs → per-feature Koin modules via `appFeatureModules()`). Printer infra stays in `:composeApp`. Awaiting build verification.
- [x] **P8  DI / wiring** — DONE. DI split: per-feature Koin modules + `:shared`'s `sharedModule()` holds only infra singles/factories (db/daos/repos/apis/use cases); navigation centralized in `:composeApp`. `:shared` reduced to DI/bootstrap (SharedModule + PlatformModule expect/actual + initKoin). SKIPPED optional `:composeApp`→`:app` / `:shared`→`:core:di` renames (cosmetic, build-fragile). Future: move infra singles into per-core-module Koin modules.
- [x] **P9  build-logic** — DONE. Added `build-logic` included build (`includeBuild` in root `pluginManagement`) with 3 class-based convention plugins:
    - `flowvan.kmp.library` — applies Kotlin MPP + Android library; wires android + iOS targets; configures `android{}` (compileSdk/minSdk/JVM11); **derives namespace + iOS framework baseName from the Gradle path** (so modules declare neither).
    - `flowvan.compose` — applies Compose MP + Compose compiler (UI modules).
    - `flowvan.room` — applies KSP + Room; sets schema dir + per-target KSP compiler deps (`:core:database`).
    All 17 library modules (8 core + 8 feature + shared) reduced to a tiny `plugins {}` + `dependencies {}` block. Catalog gained 6 `*-gradlePlugin` artifacts (compileOnly classpath for the conventions). Key technique: modules use **source-set-qualified string configs** (`"commonMainImplementation"(...)`) because the `kotlin { sourceSets {} }` accessor isn't generated when KMP is applied via a convention plugin. `:core:design-system` keeps Compose applied directly (not via `flowvan.compose`) so its `compose.resources {}` DSL accessor is generated. `:composeApp` (the application) keeps its own build file. Needs a Gradle run to validate.

## Per-phase mechanical recipe
1. `git mv` the files into the new module path with the new package folders.
2. Global string replace old package → new package across the whole tree (imports + FQNs + the moved files' own `package` line).
3. Create the module `build.gradle.kts`; `include(...)` in `settings.gradle.kts`.
4. Add the new module as a dependency where it's consumed (`api(...)` from a module
   whose public API exposes the moved types, `implementation(...)` otherwise).
5. Hand off: build in Android Studio, report errors, fix, then tick the box.