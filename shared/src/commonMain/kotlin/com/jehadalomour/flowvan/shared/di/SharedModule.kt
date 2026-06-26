package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.core.database.db.FlowVanDatabase
import com.jehadalomour.flowvan.core.database.db.buildFlowVanDatabase
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.ClaudeApiClient
import com.jehadalomour.flowvan.core.network.createHttpClient
import com.jehadalomour.flowvan.core.network.api.ApprovalApi
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.api.CollectionApi
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.InvoiceApi
import com.jehadalomour.flowvan.core.network.api.MyRouteApi
import com.jehadalomour.flowvan.core.network.api.ProductApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.api.VoucherApi
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.data.repository.LocationRepository
import com.jehadalomour.flowvan.core.data.repository.SyncRepository
import com.jehadalomour.flowvan.core.datastore.AiSettings
import com.jehadalomour.flowvan.core.datastore.SyncConfig
import com.jehadalomour.flowvan.core.data.tracking.StopDetector
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator
import com.jehadalomour.flowvan.core.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RequestReturnApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.PollApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CancelApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CommitApprovedReturnUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetCustomerSalesUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EndShiftUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.core.domain.usecase.LoginUseCase
import com.jehadalomour.flowvan.core.domain.usecase.LogoutUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RecordCollectionUseCase
import com.jehadalomour.flowvan.core.domain.usecase.VoucherNumberGenerator
import com.jehadalomour.flowvan.core.domain.usecase.BackendLoginUseCase
import com.jehadalomour.flowvan.core.domain.usecase.PurgeDemoDataUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RefreshCatalogUseCase
import com.jehadalomour.flowvan.core.domain.usecase.SubmitCollectionUseCase
import com.jehadalomour.flowvan.core.domain.usecase.SubmitInvoiceUseCase
import com.jehadalomour.flowvan.core.domain.usecase.StartShiftUseCase
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(): Module = module {
    single { Settings() }
    single { SessionStore(get()) }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
    single<FlowVanDatabase> { buildFlowVanDatabase(get()) }
    single { get<FlowVanDatabase>().userDao() }
    single { get<FlowVanDatabase>().customerDao() }
    single { get<FlowVanDatabase>().productDao() }
    single { get<FlowVanDatabase>().productUnitDao() }
    single { get<FlowVanDatabase>().invoiceDao() }
    single { get<FlowVanDatabase>().paymentDao() }
    single { get<FlowVanDatabase>().shiftDao() }
    single { get<FlowVanDatabase>().locationPointDao() }
    single { get<FlowVanDatabase>().routeStopDao() }
    single { get<FlowVanDatabase>().aiMessageDao() }
    single { get<FlowVanDatabase>().appSettingsDao() }

    single { UserRepository(get()) }
    single { CustomerRepository(get()) }
    single { AppSettingsRepository(get()) }
    single { ProductRepository(get()) }
    single { ProductUnitRepository(get()) }
    single { InvoiceRepository(get()) }
    single { PaymentRepository(get()) }
    single { LocationRepository(get()) }
    single { StopDetector() }
    single { LocationTrackingCoordinator(get(), get(), get()) }
    single { AiSettings(get()) }
    single { SyncConfig(get()) }
    single { ApiConfig(get()) }
    single { createHttpClient() }
    single { ClaudeApiClient(get()) }
    single { SyncRepository(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SyncScheduler(get(), get()) }

    // Backend API layer (VanFlow) — see .claude/FLOW-API.md
    single {
        // Dedicated request-encoding Json: omit nulls/defaults so optional fields
        // aren't sent as `null` (backend validators reject that for some fields).
        val apiJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            explicitNulls = false
        }
        FlowVanApiClient(get(), get(), get(), apiJson)
    }
    single { AuthApi(get()) }
    single { ApprovalApi(get()) }
    single { CustomerApi(get()) }
    single { ProductApi(get()) }
    single { InvoiceApi(get()) }
    single { CollectionApi(get()) }
    single { RepApi(get()) }
    single { VoucherApi(get()) }
    single { MyRouteApi(get()) }

    factory { LoginUseCase(get(), get()) }
    factory { GetCurrentUserUseCase(get(), get()) }
    factory { LogoutUseCase(get(), get()) }
    factory { GetDailyKpiUseCase(get(), get(), get()) }
    factory { VoucherNumberGenerator(get(), get()) }
    factory { CreateSaleVoucherUseCase(get(), get(), get(), get(), get(), get()) }
    factory { CreateReturnVoucherUseCase(get(), get(), get(), get(), get(), get()) }
    factory { RequestReturnApprovalUseCase(get(), get(), get()) }
    factory { PollApprovalUseCase(get()) }
    factory { CancelApprovalUseCase(get()) }
    factory { CommitApprovedReturnUseCase(get(), get(), get(), get()) }
    factory { GetCustomerSalesUseCase(get()) }
    factory { CreateRequestVoucherUseCase(get(), get(), get(), get()) }
    factory { RecordCollectionUseCase(get(), get(), get()) }
    factory { EndShiftUseCase(get()) }
    factory { StartShiftUseCase(get(), get()) }
    factory { BackendLoginUseCase(get(), get(), get()) }
    factory { PurgeDemoDataUseCase(get(), get()) }
    factory { RefreshCatalogUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SubmitInvoiceUseCase(get()) }
    factory { SubmitCollectionUseCase(get()) }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule(), platformModule())
}
