# CashFlow — KMP / Compose Multiplatform Architecture Spec
## v2.0 — Full Module Structure, UseCases, Repositories, ViewModels
**Date:** May 2026  |  **Tenant:** Al-Madina Trading Co. (Amman, Jordan)
**Audience:** mobile engineers building this app (senior level)

> v2.0 supersedes v1.0 (`CASHFLOW_MODULES.md`). v1.0 described **what** each feature does. v2.0 describes **how** it is built: Gradle modules, package layout, class contracts, DI wiring, navigation contracts, error model, test surface. Read v1.0 first for user-flow context; this doc is the engineering contract.

---

## §0. Why this document exists

A senior team does not start coding off a feature list. They first agree on:

1. The **module graph** — who depends on whom. Once this is right, parallel work is possible and accidental coupling is impossible.
2. The **layer contracts** per feature — what's a UseCase, what's a Repository, what's exposed to the UI. Once these are right, a junior can implement a feature without rethinking the architecture.
3. The **patterns** that repeat — MVI state shape, Koin module shape, navigation route shape. Once these are agreed, every new feature looks the same and reviews are fast.

This document fixes all three. Every section below is a contract, not a suggestion.

---

## §1. Tech Stack (locked to what's in `libs.versions.toml`)

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin | 2.3.21 |
| Build | AGP | 8.11.2 |
| UI | Compose Multiplatform | 1.10.3 |
| Material | Compose Material3 | 1.10.0-alpha05 |
| Targets | Android (min 24, target/compile 36), iOS 14+ | — |
| DI | Koin + koin-compose-viewmodel | 4.0.0 |
| Local DB | Room (KMP) + sqlite-bundled | 2.7.0 / 2.5.0 |
| Annotation processing | KSP | **2.3.7** (independent of Kotlin version since KSP 2.x) |
| Networking | Ktor Client | 3.0.3 |
| Navigation | androidx.navigation compose (KMP) | 2.8.0-alpha13 |
| Async | kotlinx-coroutines | 1.10.1 |
| Serialization | kotlinx-serialization-json | 1.8.0 |
| Date/Time | kotlinx-datetime | 0.6.2 |
| Settings | multiplatform-settings | 1.3.0 |
| Logging | Kermit | 2.0.5 |
| Lifecycle/ViewModel | androidx-lifecycle (KMP) | 2.10.0 |
| Location (Android) | play-services-location | 21.3.0 |

**No new dependencies may be added without architectural review.** The list above is the contract. If a feature module needs something else, it lands in `libs.versions.toml` first and the rationale lives in PR description.

---

## §2. Project Module Structure (the Gradle graph)

A v1 mistake to avoid: putting everything in one `:shared` module. That makes incremental builds slow, allows accidental cross-feature imports, and blocks teams from owning a feature. We split.

```
cashflow/
├── build-logic/                         # convention plugins (kmp.library, kmp.feature)
│   └── convention/
│
├── core/                                # cross-feature primitives — every feature can depend on these
│   ├── designsystem/                    # theme, colors, typography, base composables
│   ├── ui/                              # shared higher-level composables (KpiCard, EmptyState…)
│   ├── domain/                          # domain models + repository INTERFACES (no impls)
│   ├── data/                            # Room, Ktor, repository IMPLEMENTATIONS, seeder
│   ├── navigation/                      # typed route definitions + NavHost helper
│   ├── common/                          # utils: formatters, DateExt, Result wrappers
│   └── testing/                         # test doubles, fixtures, in-memory DAOs
│
├── feature/                             # vertical features — each is independent
│   ├── auth/                            # M03
│   ├── home/                            # M04
│   ├── route/                           # M05
│   ├── customers/                       # M06 + M07 (list + dashboard share state)
│   ├── voucher-sale/                    # M08
│   ├── voucher-return/                  # M09
│   ├── voucher-request/                 # M10
│   ├── collection/                      # M11
│   ├── ai/                              # M13
│   ├── vanstock/                        # M14
│   ├── endofday/                        # M15
│   └── location/                        # M16 (background tracking service)
│
├── androidApp/                          # Android entry point — composes the feature graph
└── iosApp/                              # SwiftUI shell + ComposeUIViewController
```

**Hard rules**

| Rule | Why |
|---|---|
| `:feature:*` modules **may not depend on each other.** | Prevents circular graphs; enforces decoupling. Cross-feature communication = navigation events bubbled up to the host. |
| `:feature:*` may only depend on `:core:*`. | Same as above, applied to every feature. |
| `:core:domain` is **pure Kotlin** — no Android, no Compose, no Room. | So it can be unit-tested instantly and reused on iOS via Swift export later. |
| `:core:data` may import `:core:domain` only. | Domain doesn't know data exists; data implements domain interfaces. Standard Clean Architecture inversion. |
| `:core:designsystem` is **Compose Multiplatform only** — no domain knowledge. | A new feature module can theme itself without pulling in business code. |
| `:core:ui` may depend on `:core:designsystem` and `:core:domain` (for model types in composables only). | UI components can render a `Customer`, but they don't fetch one. |
| Only `:androidApp` and `:iosApp` know about every feature. | They are the composition root. |

**Module dependency graph (terse form)**

```
androidApp ───────┬──> feature:auth          ──> core:designsystem, core:ui, core:domain, core:navigation, core:data
                  ├──> feature:home          ──> (same)
                  ├──> feature:route         ──> (same)
                  ├──> feature:customers     ──> (same)
                  ├──> feature:voucher-sale  ──> (same)
                  ├──> feature:voucher-return──> (same)
                  ├──> feature:voucher-request─> (same)
                  ├──> feature:collection    ──> (same)
                  ├──> feature:ai            ──> (same)
                  ├──> feature:vanstock      ──> (same)
                  ├──> feature:endofday      ──> (same)
                  └──> feature:location      ──> core:domain, core:data (no UI)

core:data       ──> core:domain
core:ui         ──> core:designsystem, core:domain
core:navigation ──> (none — just routes)
core:domain     ──> (pure Kotlin)
core:common     ──> (pure Kotlin)
```

**Per-module `build.gradle.kts` is a one-liner** that applies a convention plugin from `build-logic/`:

```kotlin
// :core:domain/build.gradle.kts
plugins { id("cashflow.kmp.library") }
```
```kotlin
// :feature:auth/build.gradle.kts
plugins { id("cashflow.kmp.feature") }
dependencies { /* feature-specific extras only */ }
```

The convention plugin handles Kotlin targets, Compose MP, Koin, lifecycle — everything every feature shares. No copy-paste across 12 build files.

---

## §3. Cross-cutting Conventions

These apply to every feature module. Repeating them in each feature section would waste your time and mine.

### 3.1 The MVI Triplet — State / Event / Effect

Every screen-level ViewModel in a `:feature:*` module owns three sealed types:

```
data class XxxState(
    val <… every observable property, default values …>,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface XxxEvent {
    // every user action — taps, text input, refresh, retry
    data class FieldChanged(val value: String) : XxxEvent
    object Submit : XxxEvent
    object Retry : XxxEvent
    …
}

sealed interface XxxEffect {
    // every one-shot side effect — navigation, snackbar, vibration
    data class NavigateTo(val route: String) : XxxEffect
    data class ShowSnackbar(val msgKey: StringResource) : XxxEffect
    …
}
```

The ViewModel exposes:

```
class XxxViewModel(<deps>) : ViewModel() {
    val state: StateFlow<XxxState>           // hot, conflated, replays 1
    val effect: SharedFlow<XxxEffect>        // hot, buffer 0, replays 0 — fire and forget
    fun onEvent(event: XxxEvent)
}
```

**Rule**: nothing else. No public functions besides `onEvent`. No public state setters. No exposed coroutine scopes.

**Why split State and Effect?** State is *what the screen looks like right now*. Effect is *something that should happen once and never replay*. If you navigate by setting `state.shouldNavigate = true`, then on config-change (orientation rotation) the user navigates again. Splitting prevents this entire class of bug.

### 3.2 UseCase pattern

```
class DoSomethingUseCase(
    private val repo: SomethingRepository,
    private val anotherRepo: AnotherRepository
) {
    // either suspend …
    suspend operator fun invoke(input: Input): Result<Output>
    // … or returns a Flow for observation
    operator fun invoke(input: Input): Flow<Output>
}
```

**Rules**
- One responsibility per UseCase. If you write `and` in its name, split it.
- UseCases are stateless — they hold no fields except injected dependencies.
- UseCases do not know about ViewModel, Compose, or Android.
- UseCases compose other UseCases freely (no DI-graph cycles since they're factories).
- Errors surface as `kotlin.Result<T>`. The UseCase never throws to its caller for *expected* failures (validation, missing data). Unexpected failures (DB corruption) may throw and bubble to a `CoroutineExceptionHandler`.

### 3.3 Repository pattern

```
// in :core:domain
interface XxxRepository {
    fun observeAll(): Flow<List<Xxx>>
    suspend fun findById(id: String): Xxx?
    suspend fun save(xxx: Xxx)
    …
}

// in :core:data
internal class XxxRepositoryImpl(
    private val dao: XxxDao,
    private val api: XxxApi?,                 // null until backend exists
    private val ioDispatcher: CoroutineDispatcher
) : XxxRepository { … }
```

**Rules**
- Interface in `:core:domain`. Implementation in `:core:data`. Domain doesn't compile against Room.
- Every IO call wrapped in `withContext(ioDispatcher)`. Default `Dispatchers.IO` injected, swappable for tests.
- Implementations are `internal` — only the Koin module can construct them.
- Repository **never returns Room entities or DTOs** — it maps to domain models at the boundary.
- Offline-first: every read is from Room. Backend, when added, refills Room and never goes direct to UI.
- Every write goes to Room **synchronously** before any network attempt. Marked `synced=false` until the sync engine (M17) acknowledges.

### 3.4 DI Pattern (Koin)

Every feature module exposes **one** Koin `Module`:

```
// :feature:xxx/src/commonMain/.../di/XxxModule.kt
val xxxModule = module {
    // UseCases
    factoryOf(::DoSomethingUseCase)
    factoryOf(::AnotherUseCase)

    // ViewModels
    viewModelOf(::XxxScreenViewModel)
    viewModel { (id: String) -> XxxDetailViewModel(id, get()) }
}
```

`:core:data` exposes `coreDataModule` (Room, Ktor, every Repository impl).
`:core:common` exposes `coreCommonModule` (formatters, Settings).

The composition root in `:androidApp` and `:iosApp` calls:

```
startKoin {
    modules(
        coreCommonModule,
        coreDataModule,
        authModule, homeModule, routeModule, customersModule,
        voucherSaleModule, voucherReturnModule, voucherRequestModule,
        collectionModule, aiModule, vanstockModule, endofdayModule, locationModule
    )
}
```

**Rule**: never use Koin's global `GlobalContext` from inside a feature module — always inject. The composition root is the only place where Koin is started.

### 3.5 Navigation Pattern

Typed routes via `@Serializable` data classes (Compose Navigation 2.8+ supports this on KMP).

```
// in :core:navigation
@Serializable object LoginRoute
@Serializable object HomeRoute
@Serializable data class CustomerDashboardRoute(val customerId: String)
@Serializable data class SaleVoucherRoute(val customerId: String)
…
```

Each feature module exposes a `NavGraphBuilder` extension that registers its destinations:

```
// :feature:auth/src/commonMain/.../navigation/AuthNavGraph.kt
fun NavGraphBuilder.authGraph(
    onLoginSuccess: () -> Unit
) {
    composable<LoginRoute> {
        LoginScreen(onLoginSuccess = onLoginSuccess)
    }
}
```

The composition root combines them:

```
NavHost(navController, startDestination = if (isLoggedIn) HomeRoute else LoginRoute) {
    authGraph(onLoginSuccess = { navController.navigate(HomeRoute) })
    homeGraph(navController)
    routeGraph(navController)
    customersGraph(navController)
    saleGraph(navController)
    …
}
```

**Rule**: a feature module never calls `navController.navigate(...)`. It emits a `XxxEffect.NavigateTo` from its ViewModel; the screen-level composable converts that into a navController call. This keeps the ViewModel free of navigation knowledge and testable in pure Kotlin.

### 3.6 Error Model

A single `sealed interface AppError` lives in `:core:domain`:

```
sealed interface AppError {
    val messageKey: String          // for i18n lookup

    // Auth
    data object PhoneNotRegistered      : AppError { override val messageKey = "err.auth.phone_not_registered" }
    data object WrongPassword           : AppError { override val messageKey = "err.auth.wrong_password" }
    data object LocationDenied          : AppError { override val messageKey = "err.auth.location_denied" }

    // Validation
    data class FieldRequired(val field: String) : AppError { override val messageKey = "err.validation.required" }
    data class InsufficientStock(
        val productName: String, val available: Int
    ) : AppError { override val messageKey = "err.stock.insufficient" }

    // Data
    data object NotFound                : AppError { override val messageKey = "err.data.not_found" }
    data object NetworkUnavailable      : AppError { override val messageKey = "err.net.unavailable" }
    data class Unknown(val cause: Throwable?) : AppError { override val messageKey = "err.unknown" }
}
```

**Rule**: ViewModels store `error: AppError?` in their State. The UI layer translates the message key into bilingual text via a `StringResource` lookup. ViewModels do not contain Arabic or English strings.

### 3.7 Threading Model

| Where | Dispatcher | Why |
|---|---|---|
| ViewModel `viewModelScope` | `Dispatchers.Main.immediate` (Compose-aware) | Composables read state on main |
| UseCase | inherits caller's context | Stateless, no dispatcher of its own |
| Repository read/write | `withContext(ioDispatcher)` | Don't block main |
| Ktor calls | Ktor manages internally | — |
| Room DAOs (suspend) | Room handles internally | — |
| `LocationTracker.observe()` | `Dispatchers.Default` | CPU work (Haversine), not IO |

`ioDispatcher` is a constructor parameter on every Repository impl, defaulting to `Dispatchers.IO`. Tests inject `UnconfinedTestDispatcher`.

### 3.8 Testing Strategy

| Layer | Test type | Location | Tools |
|---|---|---|---|
| Domain models | none (they're data classes) | — | — |
| UseCases | unit, fast | `:feature:xxx/src/commonTest` | kotlin.test + Turbine + Fakes from `:core:testing` |
| Repositories | integration | `:core:data/src/androidUnitTest` | in-memory Room + MockK for API |
| ViewModels | unit | `:feature:xxx/src/commonTest` | kotlin.test + Turbine + Fake UseCases |
| Composables | snapshot/UI | `:feature:xxx/src/androidInstrumentedTest` | Compose UI tests (Robolectric or device) |

**Rule**: every UseCase has at least one test. Every ViewModel has at least one test covering happy-path + error-path. Composables can be tested last; ViewModel tests are the safety net.

### 3.9 Bilingual & RTL Conventions

Stated once and never repeated:

- App supports `ar` (default) and `en`.
- Language toggle lives in `:core:common`; it's saved to `Settings` under `app_lang` and exposed as a `StateFlow<AppLanguage>`.
- Composables read it via `LocalAppLanguage`. RTL is set on the root via `CompositionLocalProvider(LocalLayoutDirection provides if (lang.isRtl) Rtl else Ltr)`.
- Currency = JOD, always 3 decimals, formatted by `Double.formatJod(lang)` in `:core:common`.
- Levantine month names — table lives in `:core:common/DateExt.kt`.
- Numeric values stay LTR inside RTL paragraphs — every composable that shows numbers uses `Text(..., textDirection = TextDirection.Ltr)` or wraps the substring with `\u202D…\u202C` LRE/PDF marks.

---

## §4. Module Catalog

For each functional module: the Gradle module it lives in, internal structure, contracts for every layer, and Acceptance Criteria. Cross-cutting concerns from §3 are not repeated.

---

### M01 — Foundation (`:build-logic`, `:core:*`, `:androidApp`, `:iosApp`)

**Goal:** scaffold every Gradle module, convention plugins, theme, navigation host, Koin bootstrap. No user-visible feature except a blank dark Home placeholder.

**Module list created in this phase**
- `:build-logic:convention` (custom Gradle plugins `cashflow.kmp.library` and `cashflow.kmp.feature`)
- `:core:designsystem` — `CashFlowTheme`, `CashFlowColors`, typography, base composables: `PrimaryButton`, `OutlinedField`, `EmptyState`, `LoadingState`, `ErrorState`.
- `:core:ui` — domain-aware higher-level composables (added on demand: `CustomerRow`, `KpiCard`, `TierBadge`, `SegmentChip`).
- `:core:domain` — model classes, repository interfaces (added as features need them), `AppError`, `AppLanguage`.
- `:core:data` — Room DB, DAOs, repository impls, seeder hook, Settings wrapper. The Room database is `CashFlowDatabase` with `expect/actual fun createDatabase()`.
- `:core:navigation` — typed route classes (`@Serializable` objects/data classes) and helper `rememberCashFlowNavController()`.
- `:core:common` — `formatJod()`, `DateExt`, `LevantineMonths`, `AppLanguage` provider, `Result` extensions.
- `:core:testing` — fakes, fixtures (sample `Customer`, `Product`, `Invoice` factories).

**Acceptance criteria**
- [ ] `./gradlew :androidApp:assembleDebug` succeeds.
- [ ] iOS framework links: `./gradlew :iosApp:linkPodReleaseFrameworkIosArm64`.
- [ ] `:core:domain` has zero Android/Compose imports.
- [ ] `:core:data` has zero Compose imports.
- [ ] App launches to a blank themed screen.
- [ ] Convention plugin `cashflow.kmp.feature` is applied by at least one stub feature module that produces a working empty Composable.

---

### M02 — Demo Data Seeder (inside `:core:data`)

**Goal:** seed Room on first launch so every feature can be developed before the backend exists.

**Internal structure**
```
:core:data/src/commonMain/kotlin/com/cashflow/core/data/seeder/
├── DemoSeeder.kt                    // public entry point
├── seed/
│   ├── UserSeed.kt                  // returns List<UserEntity>
│   ├── CustomerSeed.kt
│   ├── ProductSeed.kt
│   ├── InvoiceSeed.kt
│   ├── PaymentSeed.kt
│   └── ShiftSeed.kt
└── SeederFlag.kt                    // wraps Settings.getBoolean("demo_seeded")
```

**Contract**
```
class DemoSeeder(
    private val db: CashFlowDatabase,
    private val flag: SeederFlag,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun seedIfNeeded()
    suspend fun reset()                       // for debug menu / E2E tests
}
```

**Koin registration (in `coreDataModule`)**
```
singleOf(::SeederFlag)
singleOf(::DemoSeeder)
```

**Acceptance criteria** (same as v1, repeated for completeness)
- [ ] First launch seeds the DB and flips the flag.
- [ ] Second launch is a no-op (verified by log + count).
- [ ] `reset()` clears the flag and deletes seeded rows (used by debug menu only).
- [ ] All money fields stored with 3-decimal precision.
- [ ] Seeded entities pass round-trip mapping to domain models.

---

### M03 — Authentication (`:feature:auth`)

**User flow:** v1 §M03.

**Package layout**
```
:feature:auth/src/commonMain/kotlin/com/cashflow/feature/auth/
├── domain/
│   ├── LoginUseCase.kt
│   ├── GetCurrentUserUseCase.kt
│   ├── LogoutUseCase.kt
│   └── ObserveSessionUseCase.kt
├── presentation/
│   ├── LoginViewModel.kt
│   ├── LoginState.kt
│   ├── LoginEvent.kt
│   ├── LoginEffect.kt
│   └── LoginScreen.kt
├── navigation/
│   └── AuthNavGraph.kt
└── di/
    └── AuthModule.kt
```

**Repository (interface in `:core:domain`)**
```
interface AuthRepository {
    suspend fun login(phone: String, password: String, lat: Double, lng: Double): Result<User>
    suspend fun logout()
    suspend fun currentUser(): User?
    fun observeSession(): Flow<User?>
}
```

**UseCases**
```
class LoginUseCase(
    private val authRepo: AuthRepository,
    private val locationProvider: LocationProvider     // expect/actual
) {
    suspend operator fun invoke(
        phone: String,
        password: String
    ): Result<User> {
        val loc = locationProvider.lastKnownOrZero()    // never blocks login
        return authRepo.login(phone.trim(), password, loc.lat, loc.lng)
    }
}

class GetCurrentUserUseCase(private val authRepo: AuthRepository) {
    suspend operator fun invoke(): User? = authRepo.currentUser()
}

class LogoutUseCase(private val authRepo: AuthRepository) {
    suspend operator fun invoke() = authRepo.logout()
}

class ObserveSessionUseCase(private val authRepo: AuthRepository) {
    operator fun invoke(): Flow<User?> = authRepo.observeSession()
}
```

**ViewModel API**
```
class LoginViewModel(
    private val login: LoginUseCase,
    private val getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {
    val state: StateFlow<LoginState>
    val effect: SharedFlow<LoginEffect>
    fun onEvent(e: LoginEvent)
}

data class LoginState(
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val passwordVisible: Boolean = false
) {
    val canSubmit: Boolean
        get() = phone.length == 10 && phone.startsWith("07") && password.length >= 4 && !isLoading
}

sealed interface LoginEvent {
    data class PhoneChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    object TogglePasswordVisibility : LoginEvent
    object Submit : LoginEvent
    object ErrorDismissed : LoginEvent
}

sealed interface LoginEffect {
    object NavigateHome : LoginEffect
}
```

**Koin module**
```
val authModule = module {
    factoryOf(::LoginUseCase)
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::ObserveSessionUseCase)
    viewModelOf(::LoginViewModel)
}
```

**Nav graph**
```
fun NavGraphBuilder.authGraph(onLoginSuccess: () -> Unit) {
    composable<LoginRoute> {
        val vm: LoginViewModel = koinViewModel()
        LoginScreen(vm = vm, onLoginSuccess = onLoginSuccess)
    }
}
```

**Acceptance criteria**
- [ ] `LoginUseCase` test covers: success, wrong password, phone not found, location denied (logs warning, still attempts login with 0,0).
- [ ] `LoginViewModel` test using Turbine: `PhoneChanged` updates state, `Submit` toggles `isLoading`, success emits `NavigateHome` effect once.
- [ ] LoginScreen renders error chip when `state.error != null` and clears it on next `PhoneChanged`.
- [ ] Successful login persists `current_user_id` to Settings.
- [ ] App relaunch with valid session bypasses login.

---

### M04 — Home Dashboard (`:feature:home`)

**Package layout**
```
:feature:home/src/commonMain/kotlin/com/cashflow/feature/home/
├── domain/
│   ├── GetDailyKpiUseCase.kt
│   ├── ObserveTodayRouteUseCase.kt
│   └── model/
│       └── HomeBundle.kt                  // composite (user + kpi + top5)
├── presentation/
│   ├── HomeViewModel.kt
│   ├── HomeState.kt
│   ├── HomeEvent.kt
│   ├── HomeEffect.kt
│   └── HomeScreen.kt
├── navigation/
│   └── HomeNavGraph.kt
└── di/
    └── HomeModule.kt
```

**UseCases**
```
class GetDailyKpiUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val paymentRepo: PaymentRepository,
    private val customerRepo: CustomerRepository
) {
    operator fun invoke(salesmanId: String): Flow<DailyKpi>
    // composed from 3 hot flows; emits whenever any of them changes
}

class ObserveTodayRouteUseCase(
    private val customerRepo: CustomerRepository
) {
    operator fun invoke(): Flow<List<Customer>>
}
```

**ViewModel**
```
class HomeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getKpi: GetDailyKpiUseCase,
    private val observeRoute: ObserveTodayRouteUseCase,
    private val logout: LogoutUseCase
) : ViewModel()
```

**State**
```
data class HomeState(
    val user: User? = null,
    val kpi: DailyKpi = DailyKpi.EMPTY,
    val topFiveCustomers: List<Customer> = emptyList(),
    val totalRouteSize: Int = 0,
    val isLoading: Boolean = true
)

sealed interface HomeEvent {
    object Refresh : HomeEvent
    object RouteClicked : HomeEvent
    object CustomersClicked : HomeEvent
    object VanStockClicked : HomeEvent
    object AiClicked : HomeEvent
    object EndOfDayClicked : HomeEvent
    object LogoutClicked : HomeEvent
    data class CustomerClicked(val id: String) : HomeEvent
}

sealed interface HomeEffect {
    data object NavigateRoute : HomeEffect
    data object NavigateCustomers : HomeEffect
    data object NavigateVanStock : HomeEffect
    data object NavigateAi : HomeEffect
    data object NavigateEndOfDay : HomeEffect
    data class NavigateCustomerDashboard(val id: String) : HomeEffect
    data object NavigateLogout : HomeEffect
}
```

**Acceptance criteria**
- [ ] `GetDailyKpiUseCase` composes 3 flows correctly (test with Turbine and fake repos).
- [ ] Tapping any action tile emits the right `HomeEffect`.
- [ ] State `isLoading=false` after first emission from `observeRoute`.
- [ ] Pull-to-refresh re-runs use cases without re-creating the VM.

---

### M05 — Route (`:feature:route`)

**Package layout**
```
:feature:route/src/commonMain/kotlin/com/cashflow/feature/route/
├── domain/
│   ├── ObserveRouteCustomersUseCase.kt
│   ├── SearchRouteCustomersUseCase.kt
│   └── GetRouteProgressUseCase.kt
├── presentation/
│   ├── RouteViewModel.kt
│   ├── RouteState.kt
│   ├── RouteEvent.kt
│   ├── RouteEffect.kt
│   └── RouteScreen.kt
├── navigation/
│   └── RouteNavGraph.kt
└── di/
    └── RouteModule.kt
```

**UseCases**
```
class ObserveRouteCustomersUseCase(private val repo: CustomerRepository) {
    operator fun invoke(): Flow<List<Customer>>
}

class SearchRouteCustomersUseCase(private val repo: CustomerRepository) {
    operator fun invoke(query: String, scope: SearchScope): Flow<List<Customer>>
}

enum class SearchScope { ROUTE_ONLY, ALL }

class GetRouteProgressUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val customerRepo: CustomerRepository
) {
    operator fun invoke(salesmanId: String): Flow<RouteProgress>
}

data class RouteProgress(val visited: Int, val planned: Int)
```

**ViewModel**
```
class RouteViewModel(
    private val observeRoute: ObserveRouteCustomersUseCase,
    private val search: SearchRouteCustomersUseCase,
    private val progress: GetRouteProgressUseCase,
    private val session: GetCurrentUserUseCase
) : ViewModel()
```

**State**
```
data class RouteState(
    val routeCustomers: List<Customer> = emptyList(),
    val searchResults: List<Customer> = emptyList(),
    val query: String = "",
    val scope: SearchScope = SearchScope.ROUTE_ONLY,
    val progress: RouteProgress = RouteProgress(0, 0),
    val isLoading: Boolean = true
) {
    val displayList: List<Customer>
        get() = if (query.isBlank()) routeCustomers else searchResults
}

sealed interface RouteEvent {
    data class QueryChanged(val value: String) : RouteEvent
    object ToggleScope : RouteEvent
    data class CustomerClicked(val id: String) : RouteEvent
    object Back : RouteEvent
}

sealed interface RouteEffect {
    data class NavigateCustomer(val id: String) : RouteEffect
    object NavigateBack : RouteEffect
}
```

Search input is debounced **inside the ViewModel** using a `MutableStateFlow<String>().debounce(300).distinctUntilChanged()` chain — not in the composable.

**Acceptance criteria**
- [ ] Debounce verified: typing 5 chars rapidly → only one search query reaches the repo.
- [ ] `ToggleScope` switches between route-only and all-customers.
- [ ] Progress count reactive when a new invoice is saved by another feature.

---

### M06 + M07 — Customers (`:feature:customers`)

Two screens share the same feature module because they share use cases and data. (M06 = directory list; M07 = customer dashboard.)

**Package layout**
```
:feature:customers/src/commonMain/kotlin/com/cashflow/feature/customers/
├── domain/
│   ├── ObserveAllCustomersUseCase.kt
│   ├── ObserveCustomerUseCase.kt
│   ├── GetCustomerStatementUseCase.kt
│   ├── FilterCustomersUseCase.kt
│   └── model/
│       └── CustomerStatement.kt
├── presentation/
│   ├── list/
│   │   ├── CustomerListViewModel.kt
│   │   ├── CustomerListState.kt
│   │   ├── CustomerListEvent.kt
│   │   ├── CustomerListEffect.kt
│   │   └── CustomerListScreen.kt
│   └── dashboard/
│       ├── CustomerDashboardViewModel.kt
│       ├── CustomerDashboardState.kt
│       ├── CustomerDashboardEvent.kt
│       ├── CustomerDashboardEffect.kt
│       └── CustomerDashboardScreen.kt
├── navigation/
│   └── CustomersNavGraph.kt
└── di/
    └── CustomersModule.kt
```

**Key UseCase: customer statement**
```
class GetCustomerStatementUseCase(
    private val customerRepo: CustomerRepository,
    private val invoiceRepo: InvoiceRepository,
    private val paymentRepo: PaymentRepository
) {
    operator fun invoke(customerId: String): Flow<CustomerStatement>
}

data class CustomerStatement(
    val customer: Customer,
    val sales: List<Invoice>,
    val returns: List<Invoice>,
    val requests: List<Invoice>,
    val payments: List<Payment>,
    val totals: StatementTotals
)

data class StatementTotals(
    val salesTotal: Double,
    val returnsTotal: Double,
    val collectionsTotal: Double,
    val balance: Double,
    val overdue: Double
)
```

**Dashboard ViewModel**
```
class CustomerDashboardViewModel(
    customerId: String,
    private val getStatement: GetCustomerStatementUseCase
) : ViewModel()

data class CustomerDashboardState(
    val statement: CustomerStatement? = null,
    val selectedTab: CustomerTab = CustomerTab.SUMMARY,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

enum class CustomerTab { SUMMARY, SALES, RETURNS, REQUESTS, COLLECTIONS }

sealed interface CustomerDashboardEvent {
    data class TabSelected(val tab: CustomerTab) : CustomerDashboardEvent
    object SaleClicked : CustomerDashboardEvent
    object ReturnClicked : CustomerDashboardEvent
    object RequestClicked : CustomerDashboardEvent
    object CollectClicked : CustomerDashboardEvent
    object AiClicked : CustomerDashboardEvent
    data class InvoiceClicked(val id: String) : CustomerDashboardEvent
    object Back : CustomerDashboardEvent
}

sealed interface CustomerDashboardEffect {
    data class NavigateSale(val customerId: String) : CustomerDashboardEffect
    data class NavigateReturn(val customerId: String) : CustomerDashboardEffect
    data class NavigateRequest(val customerId: String) : CustomerDashboardEffect
    data class NavigateCollection(val customerId: String) : CustomerDashboardEffect
    data class NavigateAi(val customerId: String) : CustomerDashboardEffect
    data class NavigateInvoice(val invoiceId: String) : CustomerDashboardEffect
    object NavigateBack : CustomerDashboardEffect
}
```

**Koin registration**
```
val customersModule = module {
    factoryOf(::ObserveAllCustomersUseCase)
    factoryOf(::ObserveCustomerUseCase)
    factoryOf(::GetCustomerStatementUseCase)
    factoryOf(::FilterCustomersUseCase)

    viewModelOf(::CustomerListViewModel)
    viewModel { (cid: String) ->
        CustomerDashboardViewModel(customerId = cid, getStatement = get())
    }
}
```

**Acceptance criteria**
- [ ] After M08 saves a sale, the SALES tab updates within 500ms (verified by Turbine test on `GetCustomerStatementUseCase`).
- [ ] All 5 tabs sort by `createdAt DESC`.
- [ ] Empty tab renders an `EmptyState` composable from `:core:designsystem`.
- [ ] Action bar emits correct `CustomerDashboardEffect` per tap.

---

### M08 — Sale Voucher (`:feature:voucher-sale`)

**Package layout**
```
:feature:voucher-sale/src/commonMain/kotlin/com/cashflow/feature/sale/
├── domain/
│   ├── ObserveProductsUseCase.kt
│   ├── ObserveCustomerForSaleUseCase.kt
│   ├── ValidateCartStockUseCase.kt
│   ├── CalculateInvoiceTotalsUseCase.kt
│   └── CreateSaleInvoiceUseCase.kt
├── presentation/
│   ├── SaleVoucherViewModel.kt
│   ├── SaleVoucherState.kt
│   ├── SaleVoucherEvent.kt
│   ├── SaleVoucherEffect.kt
│   ├── SaleVoucherScreen.kt
│   ├── CartView.kt
│   ├── ProductPickerView.kt
│   └── PaymentMethodSheet.kt
├── navigation/
│   └── SaleNavGraph.kt
└── di/
    └── SaleModule.kt
```

**Domain models specific to this feature**
```
data class CartLine(
    val product: Product,
    val qty: Int,
    val unitPrice: Double,
    val discountPct: Double = 0.0
) {
    val lineTotal: Double
        get() = qty * unitPrice * (1.0 - discountPct / 100.0)
}

data class InvoiceTotals(
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double
)
```

**UseCases**
```
class CalculateInvoiceTotalsUseCase {
    operator fun invoke(
        lines: List<CartLine>,
        overallDiscount: Double,
        taxRate: Double = 0.16
    ): InvoiceTotals
}

class ValidateCartStockUseCase(private val productRepo: ProductRepository) {
    suspend operator fun invoke(lines: List<CartLine>): Result<Unit>
    // returns AppError.InsufficientStock(...) on failure
}

class CreateSaleInvoiceUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val productRepo: ProductRepository,
    private val customerRepo: CustomerRepository,
    private val validateStock: ValidateCartStockUseCase,
    private val calc: CalculateInvoiceTotalsUseCase
) {
    suspend operator fun invoke(input: NewSaleInput): Result<Invoice>
}

data class NewSaleInput(
    val customer: Customer,
    val salesmanId: String,
    val lines: List<CartLine>,
    val overallDiscount: Double,
    val paymentMethod: PaymentMethod,
    val notes: String?
)
```

**`CreateSaleInvoiceUseCase` logic (contract)**
1. `validateStock(lines)` — short-circuit on failure.
2. `calc(lines, overallDiscount)` → `InvoiceTotals`.
3. Build `Invoice(type=SALE, status=CONFIRMED, …)`.
4. **In a single Room transaction:**
    - Insert invoice.
    - Reduce stock for each line.
    - If `paymentMethod=CREDIT`: increase customer balance by `total`.
5. Return the persisted invoice.

The Room transaction ensures partial failures don't leave the DB in an inconsistent state.

**ViewModel state**
```
data class SaleVoucherState(
    val customer: Customer? = null,
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val overallDiscount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val totals: InvoiceTotals = InvoiceTotals.EMPTY,
    val cartVisible: Boolean = false,
    val paymentSheetVisible: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null
)
```

`totals` is **derived** but kept in state (recomputed inside the ViewModel after every cart/discount change). Don't compute it in the composable — that re-runs on recomposition and wastes work.

**Events**
```
sealed interface SaleVoucherEvent {
    object ToggleCartView : SaleVoucherEvent
    data class SearchProducts(val q: String) : SaleVoucherEvent
    data class AddProduct(val product: Product, val qty: Int = 1) : SaleVoucherEvent
    data class UpdateLineQty(val productId: String, val qty: Int) : SaleVoucherEvent
    data class UpdateLineDiscount(val productId: String, val pct: Double) : SaleVoucherEvent
    data class RemoveLine(val productId: String) : SaleVoucherEvent
    data class OverallDiscountChanged(val amount: Double) : SaleVoucherEvent
    data class NotesChanged(val notes: String) : SaleVoucherEvent
    object OpenPaymentSheet : SaleVoucherEvent
    object ClosePaymentSheet : SaleVoucherEvent
    data class PaymentMethodSelected(val method: PaymentMethod) : SaleVoucherEvent
    object Confirm : SaleVoucherEvent
    object Back : SaleVoucherEvent
}

sealed interface SaleVoucherEffect {
    data class Saved(val invoiceNumber: String) : SaleVoucherEffect
    data class Error(val error: AppError) : SaleVoucherEffect
    object NavigateBack : SaleVoucherEffect
}
```

**Koin**
```
val saleModule = module {
    factoryOf(::ObserveProductsUseCase)
    factoryOf(::ObserveCustomerForSaleUseCase)
    factoryOf(::ValidateCartStockUseCase)
    factoryOf(::CalculateInvoiceTotalsUseCase)
    factoryOf(::CreateSaleInvoiceUseCase)
    viewModel { (cid: String) ->
        SaleVoucherViewModel(cid, get(), get(), get(), get(), get())
    }
}
```

**Acceptance criteria**
- [ ] `CalculateInvoiceTotalsUseCase` unit test with multiple scenarios (no discount, line discount, overall discount, both).
- [ ] `ValidateCartStockUseCase` returns `InsufficientStock` for out-of-stock product.
- [ ] `CreateSaleInvoiceUseCase` test: success path persists invoice, reduces stock by exact qty, increases balance only when method=CREDIT.
- [ ] `CreateSaleInvoiceUseCase` test: insufficient stock → no DB writes (transaction rollback).
- [ ] ViewModel test: `AddProduct` then `Confirm` emits `Saved` effect.

---

### M09 — Return Voucher (`:feature:voucher-return`)

Mirrors M08 with three differences:
- No payment method.
- No overall discount.
- Mandatory `ReturnReason` enum (`EXPIRED, DAMAGED, WRONG_ORDER, OTHER`).

**UseCase**
```
class CreateReturnInvoiceUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val productRepo: ProductRepository,
    private val customerRepo: CustomerRepository,
    private val calc: CalculateInvoiceTotalsUseCase
) {
    suspend operator fun invoke(input: NewReturnInput): Result<Invoice>
}

data class NewReturnInput(
    val customer: Customer,
    val salesmanId: String,
    val lines: List<CartLine>,
    val reason: ReturnReason,
    val extraNotes: String?
)

enum class ReturnReason(val labelKey: String) {
    EXPIRED("return.reason.expired"),
    DAMAGED("return.reason.damaged"),
    WRONG_ORDER("return.reason.wrong_order"),
    OTHER("return.reason.other")
}
```

**Behaviour**
- Stock increases on save.
- Customer balance **decreases** by `total`.
- `notes = "سبب: ${reason} — ${extraNotes ?: ""}"`.
- Number prefix `RET-`.

Acceptance criteria parallel to M08.

---

### M10 — Request Voucher (`:feature:voucher-request`)

**UseCase**
```
class CreateRequestInvoiceUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val calc: CalculateInvoiceTotalsUseCase
) {
    suspend operator fun invoke(input: NewRequestInput): Result<Invoice>
}

data class NewRequestInput(
    val customer: Customer,
    val salesmanId: String,
    val lines: List<CartLine>,
    val expectedDeliveryDate: LocalDate?,
    val notes: String?
)
```

**Behaviour**
- No stock change. No balance change.
- May include products with `vanStock = 0` (pre-order).
- Number prefix `REQ-`.
- Status `CONFIRMED` (later: `FULFILLED` when converted to sale, v1.1).

Acceptance criteria: persist invoice; stock and balance untouched.

---

### M11 — Collection (`:feature:collection`)

**Package layout**
```
:feature:collection/src/commonMain/kotlin/com/cashflow/feature/collection/
├── domain/
│   ├── ObserveCustomerForCollectionUseCase.kt
│   ├── ValidateCollectionUseCase.kt
│   └── CreateCollectionUseCase.kt
├── presentation/
│   ├── CollectionViewModel.kt
│   ├── CollectionState.kt
│   ├── CollectionEvent.kt
│   ├── CollectionEffect.kt
│   └── CollectionScreen.kt
├── navigation/
│   └── CollectionNavGraph.kt
└── di/
    └── CollectionModule.kt
```

**UseCases**
```
class ValidateCollectionUseCase {
    operator fun invoke(input: NewCollectionInput): Result<Unit>
    // returns AppError.FieldRequired or AppError.InvalidAmount
}

class CreateCollectionUseCase(
    private val paymentRepo: PaymentRepository,
    private val customerRepo: CustomerRepository,
    private val validate: ValidateCollectionUseCase
) {
    suspend operator fun invoke(input: NewCollectionInput): Result<Payment>
}

data class NewCollectionInput(
    val customer: Customer,
    val salesmanId: String,
    val amount: Double,
    val method: PaymentMethod,           // CASH | CHEQUE | TRANSFER
    val chequeNumber: String? = null,
    val chequeBank: String? = null,
    val chequeDate: LocalDate? = null,
    val transferRef: String? = null,
    val notes: String? = null
)
```

**Validation rules** (inside `ValidateCollectionUseCase`)
- `amount > 0` → otherwise `AppError.InvalidAmount`.
- If `method == CHEQUE`: `chequeNumber.isNotBlank() && chequeBank.isNotBlank()` → otherwise `AppError.FieldRequired("cheque_number"|"cheque_bank")`.
- If `method == TRANSFER`: `transferRef.isNotBlank()` → otherwise `AppError.FieldRequired("transfer_ref")`.
- `amount > customer.balance` is a **warning, not a blocker** — emit a `WarningEffect` but allow save.

**State**
```
data class CollectionState(
    val customer: Customer? = null,
    val amountText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val chequeNumber: String = "",
    val chequeBank: String = "",
    val chequeDate: LocalDate? = null,
    val transferRef: String = "",
    val notes: String = "",
    val advancePaymentWarning: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null
)
```

Amount is stored as `String` until validation (avoids tricky `Double` formatting during typing). Parsed at the validation step.

**Effects**
```
sealed interface CollectionEffect {
    data class Saved(val receiptNumber: String) : CollectionEffect
    object NavigateBack : CollectionEffect
}
```

**Acceptance criteria**
- [ ] Cash collection without cheque fields succeeds.
- [ ] Cheque without number → save blocked with `FieldRequired("cheque_number")`.
- [ ] Customer balance reduces in repository on save (verified by Turbine on `ObserveCustomerUseCase`).
- [ ] Advance payment (amount > balance) shows warning but is saved.

---

### M13 — AI Assistant (`:feature:ai`)

**Package layout**
```
:feature:ai/src/commonMain/kotlin/com/cashflow/feature/ai/
├── domain/
│   ├── SendAiMessageUseCase.kt
│   ├── ObserveConversationUseCase.kt
│   └── model/
│       ├── AiContext.kt
│       └── AiQuickAction.kt
├── data/
│   ├── DemoAiResponder.kt              // local keyword matcher (this module)
│   └── AiGatewayClient.kt              // placeholder; real impl in M18
├── presentation/
│   ├── AiAssistantViewModel.kt
│   ├── AiAssistantState.kt
│   ├── AiAssistantEvent.kt
│   ├── AiAssistantEffect.kt
│   └── AiAssistantScreen.kt
├── navigation/
│   └── AiNavGraph.kt
└── di/
    └── AiModule.kt
```

Note: this is the **only feature module** that has its own `data/` package. Reason: `DemoAiResponder` is a placeholder implementation that ships with the feature today and gets swapped for the real gateway in M18. Keeping it inside `:feature:ai` makes the swap a single-file change.

**UseCase**
```
class SendAiMessageUseCase(
    private val responder: AiResponder,
    private val aiMessageDao: AiMessageDao
) {
    suspend operator fun invoke(
        conversationId: String,
        userText: String,
        context: AiContext
    ): Flow<AiMessage>
    // emits user message immediately, then thinking placeholder, then final response
}

interface AiResponder {
    suspend fun respond(query: String, context: AiContext): String
}

data class AiContext(
    val customerId: String?,            // null when launched from Home
    val salesmanId: String,
    val lang: AppLanguage
)
```

`DemoAiResponder` is the default `AiResponder` impl, registered today. `RemoteAiResponder` (M18) replaces it via Koin.

**Acceptance criteria**
- [ ] Demo responder returns expected response for each keyword (`ملخص`, `مبيعات`, `مخزون`, `مسار`, `عميل`).
- [ ] Customer-context entry produces a different `conversationId` than Home entry.
- [ ] Messages persist via `AiMessageDao` and reappear on relaunch.
- [ ] Swapping `AiResponder` impl in Koin compiles without UI changes.

---

### M14 — Van Stock (`:feature:vanstock`)

**Package layout** (lighter than vouchers since it's read-only)
```
:feature:vanstock/src/commonMain/kotlin/com/cashflow/feature/vanstock/
├── domain/
│   ├── ObserveProductsForStockUseCase.kt
│   ├── ObserveLowStockUseCase.kt
│   ├── GetInventoryValueUseCase.kt
│   └── model/
│       └── ProductDisplayMode.kt
├── presentation/
│   ├── VanStockViewModel.kt
│   ├── VanStockState.kt
│   ├── VanStockEvent.kt
│   ├── VanStockEffect.kt
│   └── VanStockScreen.kt
├── navigation/
│   └── VanStockNavGraph.kt
└── di/
    └── VanStockModule.kt
```

**UseCase**
```
class GetInventoryValueUseCase(private val productRepo: ProductRepository) {
    operator fun invoke(): Flow<Double>
    // sum of vanStock * salePrice across all products
}
```

**Acceptance criteria**
- [ ] Stock updates in real time when M08 saves a sale (Turbine test).
- [ ] Low-stock products appear at the top of each category section.
- [ ] Inventory value matches Σ (vanStock × salePrice).

---

### M15 — End of Day (`:feature:endofday`)

**UseCase**
```
class GetEndOfDaySummaryUseCase(
    private val invoiceRepo: InvoiceRepository,
    private val paymentRepo: PaymentRepository,
    private val customerRepo: CustomerRepository
) {
    suspend operator fun invoke(salesmanId: String): EndOfDaySummary
}

data class EndOfDaySummary(
    val sales: KpiAmount,
    val returns: KpiAmount,
    val cashCollected: Double,
    val chequesCollected: Double,
    val transfersCollected: Double,
    val customersVisited: Int,
    val customersPlanned: Int,
    val unsyncedInvoiceCount: Int,
    val unsyncedPaymentCount: Int
)

data class KpiAmount(val total: Double, val count: Int)

class EndShiftUseCase(
    private val shiftRepo: ShiftRepository,
    private val logout: LogoutUseCase
) {
    suspend operator fun invoke(salesmanId: String): Result<Unit>
}
```

**Acceptance criteria**
- [ ] Cash collected = sum of payments today where `method=CASH`.
- [ ] `EndShiftUseCase` ends the shift row and logs out atomically (one transaction).

---

### M16 — Location Tracking (`:feature:location`)

This module has **no UI** — it's a background service triggered by the auth + endofday flows.

**Package layout**
```
:feature:location/src/commonMain/kotlin/com/cashflow/feature/location/
├── domain/
│   ├── StartShiftTrackingUseCase.kt
│   ├── StopShiftTrackingUseCase.kt
│   ├── ObserveStopsUseCase.kt
│   └── ObserveOffRouteUseCase.kt
├── data/
│   ├── LocationTracker.kt              // expect
│   ├── StopDetector.kt                 // pure Kotlin Haversine
│   └── RouteValidator.kt               // pure Kotlin geofence
├── di/
│   └── LocationModule.kt
└── ────────────────────────────────────
:feature:location/src/androidMain/kotlin/com/cashflow/feature/location/
└── data/
    ├── LocationTracker.android.kt      // FusedLocationProviderClient impl
    └── TrackingForegroundService.kt
:feature:location/src/iosMain/kotlin/com/cashflow/feature/location/
└── data/
    └── LocationTracker.ios.kt          // CLLocationManager impl
```

**expect/actual contract**
```
// commonMain
expect class LocationTracker {
    fun observe(intervalMs: Long, minDistanceM: Float): Flow<LocationPoint>
    suspend fun lastKnown(): LocationPoint?
}
```

**UseCases**
```
class StartShiftTrackingUseCase(
    private val tracker: LocationTracker,
    private val locationRepo: LocationRepository,
    private val shiftRepo: ShiftRepository,
    private val stopDetector: StopDetector,
    private val routeValidator: RouteValidator
) {
    suspend operator fun invoke(salesmanId: String): Result<Shift>
    // - starts shift, persists Shift row
    // - launches tracker.observe(…) on a background coroutine
    // - each point: write to Room + feed StopDetector + feed RouteValidator
}

class StopShiftTrackingUseCase(
    private val tracker: LocationTracker,
    private val shiftRepo: ShiftRepository
) {
    suspend operator fun invoke(shiftId: String): Result<Unit>
}
```

**Acceptance criteria** (testable bits)
- [ ] `StopDetector` unit test with synthetic point stream: ≥50m drift for <3min → no stop. Within 50m for ≥3min → emits exactly one stop.
- [ ] `RouteValidator` unit test: point >300m from any route segment → `OffRouteEvent`. Within 300m → no event.
- [ ] Android: foreground service notification appears and persists across app background.
- [ ] All points written to Room with `synced=false` (verified by DAO query).

---

## §5. Build Phases (with module deliverables per phase)

| Phase | Modules built | Status |
|---|---|---|
| **P1** Foundation | `:build-logic`, all `:core:*`, `:androidApp` shell, `:iosApp` shell, M02 seeder | required |
| **P2** Auth + shell | `:feature:auth` (M03), `:feature:home` (M04) | required |
| **P3** Customer surface | `:feature:route` (M05), `:feature:customers` (M06+M07) | required |
| **P4** Transactions | `:feature:voucher-sale` (M08), `:feature:voucher-return` (M09), `:feature:voucher-request` (M10), `:feature:collection` (M11) | required |
| **P5** Smart features | `:feature:ai` (M13), `:feature:vanstock` (M14), `:feature:endofday` (M15) | required |
| **P6** Tracking | `:feature:location` (M16) | required |
| **P7** Backend | M17 sync engine, M18 real AI | deferred until API exists |

**Rule:** never start the next phase until every required module in the current phase passes Acceptance Criteria.

---

## §6. Module Dependency Map (functional view)

```
                                     ┌──────────────────────────────┐
                                     │  :core:domain  (interfaces)  │
                                     └──────────────┬───────────────┘
                                                    │
                                         implements │
                                                    ▼
                                     ┌──────────────────────────────┐
                                     │  :core:data  (Room + repos)  │
                                     └──────────────┬───────────────┘
                                                    │
       ┌──────────────┬──────────────┬──────────────┼──────────────┬──────────────┬──────────────┐
       ▼              ▼              ▼              ▼              ▼              ▼              ▼
   feature:auth   feature:home   feature:route  feature:customers  feature:voucher-*  feature:collection  …
       │              │              │              │              │              │
       └──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
                                                    │
                                                    ▼
                                                androidApp / iosApp
                                            (composition root: Koin + NavHost)
```

---

## §7. Definition of Done (v1.0 release)

1. Every required module M01–M16 has its Gradle module, package structure, contracts, and Acceptance Criteria green.
2. `./gradlew assembleDebug` and `linkPodReleaseFrameworkIosArm64` both succeed from a clean checkout.
3. `:core:domain` has zero Android/Compose imports (verified by CI rule).
4. Every `:feature:*` module is self-contained: a "build the feature in isolation" verification task succeeds.
5. Every UseCase has at least one unit test. Every ViewModel has at least two.
6. Bilingual AR/EN works on both platforms; RTL layout correct.
7. Full salesman day flow runs offline: login → route → 10 customers each with sale/return/request/collection → end of day.
8. No crash in a 30-min smoke test.
9. App launches in < 1.5s on mid-range device.
10. Battery use over an 8h shift: ≤ 8%.

---

## §8. How to use this doc with an AI assistant

For each module, prompt:

> "Implement module **M08 :feature:voucher-sale** per CASHFLOW_MODULES_v2.md §M08. Dependencies (`:core:domain`, `:core:data`, `:core:designsystem`, `:core:ui`, `:core:navigation`, `:feature:customers` for customer model only via `:core:domain`) are already in place. Produce every file listed in the package layout. Honour MVI conventions in §3.1, DI pattern in §3.4, navigation pattern in §3.5. Include unit tests for `CalculateInvoiceTotalsUseCase`, `ValidateCartStockUseCase`, `CreateSaleInvoiceUseCase`, and `SaleVoucherViewModel`. Stop when Acceptance Criteria are met."

That gives the assistant: scope, dependencies, deliverables, contract, stop condition, test surface. No guessing.

---

## §9. Out of scope for v1.0 (unchanged from v1)

Customer creation in app · signature capture · PDF export · barcode scanning · biometric login · multi-tenant workspace selector · push notifications · manager dashboard · drag-reorder route · cheque OCR · voice order · real AI streaming · map view with live pins · SQLCipher · third language.

---

## §10. Senior Tips & Gotchas

Things that bit me on previous KMP/CMP shipments and will bite you too if not flagged. Read once, refer back during PR review.

### 10.1 Build & tooling hygiene

- **`gradle.properties` baseline.** Put this on day one — adds ~30% to incremental build speed:
  ```properties
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configureondemand=true
  org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC
  kotlin.daemon.jvmargs=-Xmx3g
  kotlin.incremental.useClasspathSnapshot=true
  kotlin.code.style=official
  android.useAndroidX=true
  android.nonTransitiveRClass=true
  ```
- **Configuration cache: love it or turn it off, don't half-use it.** If you see "Timeout waiting to lock Configuration Cache", you have a stale lock from a killed build. Fix:
  ```bash
  ./gradlew --stop
  rm -rf .gradle/configuration-cache
  ```
  If it keeps recurring, disable with `org.gradle.configuration-cache=false`. Costs ~30% configure-time, saves the headache.
- **`./gradlew --stop` is your friend.** Anytime weird shit happens: daemon crash, Out of Memory, locked file, IDE drift — stop daemons first, then think.
- **JDK 17 minimum.** Kotlin 2.3+ runs fine on JDK 17 or 21. Anything older = wasted hours.
- **KSP versions are independent from Kotlin** since KSP 2.x. Don't write `ksp = "{kotlin}-{x.y.z}"`. Check Maven Central for the latest `2.x.y` published artifact.

### 10.2 Compose Multiplatform reality check

- **iOS Compose is stable but not Android Compose.** A few APIs are missing or behave differently. Test on iOS Simulator early — don't wait until the end of P1.
- **`ComposeUIViewController` is the iOS entry point.** Set its tint, RTL flag, and font resources up-front in `iosApp/iosApp/iOSApp.swift` — there's no `LocalContext` equivalent that gives you these for free.
- **Resources go through `compose.components.resources`**, not Android `R.string.xxx`. Generated as `Res.string.foo`. Put strings in `:core:designsystem/src/commonMain/composeResources/values/strings.xml` and `values-ar/strings.xml`.
- **Don't use AndroidX-only libraries in commonMain.** If a Compose lib publishes only `androidx.*` artifacts (no `org.jetbrains.compose.*`), it's Android-only. Coil 3, Ktor 3, Koin 4, AndroidX Lifecycle 2.10, AndroidX Navigation Compose 2.8 are all KMP-ready.
- **iOS framework name + linkage:**
  ```kotlin
  iosTarget.binaries.framework {
      baseName = "Shared"      // must match what iOSApp.swift imports
      isStatic = true          // dynamic = linker headaches
  }
  ```
- **Don't pass Compose `@Composable` lambdas across the iOS bridge.** Pass plain Kotlin lambdas; render the Composable on the Compose side.

### 10.3 Room KMP gotchas

- **Use `BundledSQLiteDriver` on both platforms.** Don't use the Android-only driver — it breaks iOS:
  ```kotlin
  Room.databaseBuilder<CashFlowDatabase>(/* … */)
      .setDriver(BundledSQLiteDriver())
      .setQueryCoroutineContext(Dispatchers.IO)   // critical — without this, queries leak to main
      .build()
  ```
- **DAO functions must be `suspend` or return `Flow`.** Blocking DAO functions only work on Android, not iOS Kotlin/Native.
- **`fallbackToDestructiveMigration(dropAllTables = true)`** is fine during P1–P4. Add real migrations before P7 — never in production with destructive fallback on.
- **Export schemas** to `:core:data/schemas/` and commit them. They're the diff that catches accidental schema changes during PR review.
- **One Room database, not one per feature.** Even though features are modular, the DB is shared. `CashFlowDatabase` lives in `:core:data`; DAOs are exposed via repository interfaces in `:core:domain`.
- **Cross-platform DAOs:** Room 2.7 KMP doesn't support `@RawQuery` or `@MapInfo`. Stick to `@Query`, `@Insert`, `@Update`, `@Delete`, `@Transaction`.

### 10.4 State management patterns

- **Combine multiple flows into one State with `combine + stateIn`:**
  ```kotlin
  val state: StateFlow<HomeState> = combine(
      observeUser(),
      observeKpi(salesmanId),
      observeRoute()
  ) { user, kpi, route ->
      HomeState(user, kpi, route.take(5), totalRouteSize = route.size, isLoading = false)
  }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),    // survives config change
      initialValue = HomeState()
  )
  ```
  The `WhileSubscribed(5_000)` is the magic — keeps the flow alive 5s after the last subscriber drops, so orientation changes don't tear down upstream work.
- **Always use `update { it.copy(...) }` on `MutableStateFlow`**, never `value = `. The `update` form is atomic; `value =` races on concurrent updates.
- **Effects via `Channel` then `receiveAsFlow()`**, not `SharedFlow`. Channels buffer one-shot events that *must* be consumed exactly once. `SharedFlow(replay=0)` drops events if no collector is active:
  ```kotlin
  private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
  val effect: Flow<HomeEffect> = _effect.receiveAsFlow()
  ```
- **Collect effects with `LaunchedEffect(Unit)` + `flowWithLifecycle(...)`** so you don't double-collect on orientation change.
- **Don't mix State and Event handling.** ViewModel exposes `state`, `effect`, and `onEvent`. Nothing else public. If you find yourself adding a `fun refresh()`, it's a missed `RefreshClicked` event.

### 10.5 Navigation pitfalls

- **Typed routes need `kotlinx-serialization` plugin** applied in the feature module that declares them. Forget this and the build error is misleading ("`@Serializable` cannot be applied").
- **Pass IDs, not domain objects.** A `Customer` can be 50+ fields; serialising it through navArgs is wasteful and brittle. Pass `customerId: String` and re-observe.
- **`popUpTo` with `inclusive = true` is what you want for logout**, not without. Without inclusive, the destination remains on the stack:
  ```kotlin
  navController.navigate(LoginRoute) {
      popUpTo(0) { inclusive = true }
  }
  ```
- **Don't expose the `NavController` to feature modules.** Features emit `XxxEffect.NavigateXxx`; the composition root translates it. This keeps features testable without a `TestNavHostController`.
- **Deep links come later (v1.1).** Don't design routes around them now or you'll over-engineer the URL grammar.

### 10.6 Compose performance — only these matter for v1.0

- **`LazyColumn` items always need a `key`.** Without it, scrolling 100+ rows recomposes everything on data update:
  ```kotlin
  items(customers, key = { it.id }) { customer -> CustomerRow(customer) }
  ```
- **`derivedStateOf` for computed values that depend on state:**
  ```kotlin
  val canSubmit by remember {
      derivedStateOf { phone.length == 10 && password.length >= 4 }
  }
  ```
  Reads only re-fire when the *derived* value changes, not every recomposition.
- **Mark domain models `@Immutable` (or use `data class` with only `val`s and stable types).** Compose skips recomposition for stable params. `List<Customer>` is not stable — wrap with `ImmutableList` from `kotlinx.collections.immutable` if it's a hot path.
- **Avoid passing `Any?` or `() -> Unit` directly in tight loops.** Pull lambdas to `remember` blocks. Pull `Modifier` chains outside `items {}`.
- **Don't preview-mode panic.** Compose Preview is Android-only; CMP commonMain has no `@Preview` in iOS. Use `@Preview` in `androidMain` Compose Preview source set only.

### 10.7 RTL and bilingual reality

- **`LocalLayoutDirection` is the single source of truth.** Don't sniff locale strings. Provide it at the root once:
  ```kotlin
  val lang by appLanguage.collectAsState()
  CompositionLocalProvider(
      LocalLayoutDirection provides if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
  ) { CashFlowNavGraph(...) }
  ```
- **`start`/`end` paddings flip automatically. `left`/`right` paddings don't.** Use `start`/`end` 99% of the time. The 1% (e.g. logo that must stay on the left) uses `absoluteLeft`/`absoluteRight`.
- **Numbers in RTL paragraphs stay LTR with `TextDirection.Ltr`** on a sub-Text, or by wrapping with bidi marks:
  ```kotlin
  Text("\u200E" + amount.formatJod(lang) + "\u200E")
  ```
  The `\u200E` (LRM, Left-to-Right Mark) keeps the JOD amount reading naturally in an Arabic sentence.
- **Levantine month names** matter for credibility. Hardcoded table in `:core:common/DateExt.kt`. Never auto-localise via JDK — JDK Arabic uses Gulf names (أبريل/مايو) which look foreign to a Jordanian.
- **Test RTL with English text too.** Some bugs only show up when latin text is in an RTL frame — confirm it doesn't accidentally mirror code blocks, invoice numbers, or barcodes.

### 10.8 Testing patterns

- **`runTest { }`, not `runBlocking`** — `runTest` advances virtual time, skips delays, and fails on uncaught exceptions:
  ```kotlin
  @Test
  fun loginSuccess() = runTest {
      val vm = LoginViewModel(...)
      vm.onEvent(LoginEvent.PhoneChanged("0791234567"))
      vm.onEvent(LoginEvent.PasswordChanged("1234"))
      vm.onEvent(LoginEvent.Submit)
      vm.effect.test { assertEquals(LoginEffect.NavigateHome, awaitItem()) }
  }
  ```
- **Inject `CoroutineDispatcher`, never reference `Dispatchers.IO` directly.** Tests then inject `UnconfinedTestDispatcher` or `StandardTestDispatcher`.
- **Fakes > mocks for repositories.** A `FakeCustomerRepository` in `:core:testing` is more readable than five `every { repo.observe(any()) } returns flowOf(...)` setups, and works in commonTest (where MockK doesn't always run).
- **Turbine for Flow:**
  ```kotlin
  vm.state.test {
      assertEquals(LoginState(), awaitItem())              // initial
      vm.onEvent(LoginEvent.PhoneChanged("079..."))
      assertEquals("079...", awaitItem().phone)
      cancelAndIgnoreRemainingEvents()
  }
  ```
- **Don't test composables before P5.** ViewModel + UseCase tests catch 95% of bugs. Composable tests are slow, fragile, and don't justify their cost until UI is stable.

### 10.9 Koin tips

- **`viewModelOf(::Foo)` over `viewModel { Foo(get()) }`** when possible — it's type-safe; if `Foo` adds a constructor param, the build fails at the registration site, not at runtime.
- **Parametric ViewModels:**
  ```kotlin
  viewModel { (id: String) -> CustomerDashboardViewModel(id, get()) }
  // Composable:
  val vm: CustomerDashboardViewModel = koinViewModel(parameters = { parametersOf(customerId) })
  ```
- **`single` vs `factory`:** Repositories are `single` (one DB connection, one cache). UseCases are `factory` (stateless, one per inject). ViewModels are `viewModel` (lifecycle-bound).
- **Don't use `KoinComponent` (the `get<T>()` mixin) in production code.** Inject through constructors. `KoinComponent` is a test-time convenience or a tiny-script shortcut, not a pattern.
- **Module dependencies are unordered.** If `:feature:auth/AuthModule` is missing a dependency, the failure is at first `get<Foo>()` call, not at `startKoin`. Run `checkModules()` in a unit test to catch this at build time.

### 10.10 Versioning & dependency hygiene

- **Pin every version in `libs.versions.toml`.** No `+` ranges, no `latest.release`. Reproducible builds = no 3am breakage.
- **Bump one library at a time** in dedicated PRs. If you bump Kotlin + Compose + AGP together and something breaks, you have a tri-variable bug to bisect.
- **Verify version triplets** before bumping:
    - Kotlin ↔ KSP — check https://github.com/google/ksp/releases
    - Kotlin ↔ Compose Compiler — check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html
    - AGP ↔ Kotlin — check https://kotlinlang.org/docs/gradle-configure-project.html
- **Alpha / beta dependencies (`material3 = "1.10.0-alpha05"`, `navigationCompose = "2.8.0-alpha13"`)** can be yanked or change API at any time. Acceptable in P1–P4; move to stable before P7.

### 10.11 iOS specifics that nobody warns you about

- **Background location requires `Background Modes → Location updates` in the iOS target**, not just Info.plist.
- **`NSLocationAlwaysAndWhenInUseUsageDescription`** is the key Apple actually checks. Without it the permission dialog silently doesn't appear.
- **`UISceneActivationStateBackground` doesn't pause Compose** — your VMs keep running. Stop the LocationTracker explicitly on background if battery is the concern.
- **Date formatting:** `kotlinx-datetime` doesn't have locale formatting on iOS. Use `NSDateFormatter` via `expect/actual` for any date that needs to be human-readable.
- **`pod install` after every framework rename.** The CocoaPods plugin caches frameworks under their `baseName`.

### 10.12 Debug menu — build it on day one

- Add a hidden screen accessible by long-pressing the logo on Home in `BuildConfig.DEBUG` builds.
- Should contain at minimum:
    - Re-run seeder
    - Clear DB
    - Toggle language AR ↔ EN
    - Dump current user / current shift to Logcat
    - Force crash (test Crashlytics / Sentry when added)
    - Switch `AiResponder` impl (demo vs gateway) once M18 lands
- Saves you 90% of "can you reproduce the seeded state" pain during QA.

### 10.13 PR / review checklist (post on the team wiki)

Before approving any feature PR, the reviewer checks:

- [ ] Feature lives in its own `:feature:*` module.
- [ ] No imports from another `:feature:*` module.
- [ ] No `Dispatchers.IO` reference outside `:core:data`.
- [ ] Every `MutableStateFlow.update { ... }`, no `.value = ...` in coroutines.
- [ ] Every `LazyColumn` and `LazyRow` `items {}` has a `key`.
- [ ] No Arabic / English strings hard-coded in commonMain Kotlin — only in resources.
- [ ] All money/totals at 3 decimal places.
- [ ] Every new UseCase has a test.
- [ ] Every new ViewModel has a happy-path test and an error-path test.
- [ ] No `runBlocking { }` outside Application bootstrap (and ideally not even there — use App Startup).
- [ ] Acceptance Criteria from the module spec are checked off in PR description.

### 10.14 When the architecture rule fights you, the architecture rule wins

Two anti-patterns to refuse in code review:

1. **"Feature A needs to call Feature B."** No. Bubble the action up to the composition root via a navigation effect. If they truly share logic, extract a `:core:` module.
2. **"Just inject the DB into the ViewModel for this one screen."** No. Repository contract or new UseCase. Every shortcut here is six months of unwinding later.

The rules in §2 exist because every team that broke them shipped slower than the team that didn't.

---

## §11. Glossary

| Term | Meaning |
|---|---|
| **KMP** | Kotlin Multiplatform — share Kotlin code between Android, iOS, JVM, JS, etc. |
| **CMP** | Compose Multiplatform — Compose UI running on multiple targets, not just Android. |
| **MVI** | Model–View–Intent. Our flavour: State (one immutable data class) + Event (sealed interface for user actions) + Effect (sealed interface for one-shot side effects). |
| **expect / actual** | The KMP mechanism for declaring a platform-agnostic API in `commonMain` and providing platform-specific impls in `androidMain` / `iosMain`. |
| **Composition root** | The single place where DI is wired and the navigation graph is built. In this project: `:androidApp` and `:iosApp`. |
| **Convention plugin** | A custom Gradle plugin in `:build-logic` that turns every feature's `build.gradle.kts` into a one-liner. |
| **Hot flow** | A `Flow` that is alive independent of collectors (e.g. `StateFlow`, `SharedFlow`). Versus cold flows that start work per collector. |
| **Stable / Immutable** | Compose annotations that let the runtime skip recomposition when params haven't changed. |
| **Acceptance Criteria** | Boolean conditions that *must* be green for a module to be considered done. Not aspirational; not negotiable. |

---

*End of CashFlow Architecture Spec v2.0.*