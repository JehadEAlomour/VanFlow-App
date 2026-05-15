package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.shared.data.local.db.FlowVanDatabase
import com.jehadalomour.flowvan.shared.data.local.db.buildFlowVanDatabase
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.data.repository.PaymentRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.data.repository.UserRepository
import com.jehadalomour.flowvan.shared.data.seeder.DemoSeeder
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.EndShiftUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LoginUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LogoutUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.RecordCollectionUseCase
import com.jehadalomour.flowvan.shared.presentation.feature.accountstatement.AccountStatementViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.ai.AiAssistantViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.map.MapNavigationViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.paymentreport.PaymentReportViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.receiptdetail.ReceiptDetailViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.transactionreport.TransactionReportViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.voucherdetail.VoucherDetailViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.collection.CollectionViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.customers.CustomerListViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.endofday.EndOfDayViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.home.HomeViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.login.LoginViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.request.RequestVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.returns.ReturnVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.route.RouteViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.vanstock.VanStockViewModel
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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
    single { get<FlowVanDatabase>().invoiceDao() }
    single { get<FlowVanDatabase>().paymentDao() }
    single { get<FlowVanDatabase>().shiftDao() }
    single { get<FlowVanDatabase>().locationPointDao() }
    single { get<FlowVanDatabase>().routeStopDao() }
    single { get<FlowVanDatabase>().aiMessageDao() }

    single { UserRepository(get()) }
    single { CustomerRepository(get()) }
    single { ProductRepository(get()) }
    single { InvoiceRepository(get()) }
    single { PaymentRepository(get()) }
    single { DemoSeeder(get(), get(), get()) }

    factory { LoginUseCase(get(), get()) }
    factory { GetCurrentUserUseCase(get(), get()) }
    factory { LogoutUseCase(get()) }
    factory { GetDailyKpiUseCase(get(), get(), get()) }
    factory { CreateSaleVoucherUseCase(get(), get(), get(), get()) }
    factory { CreateReturnVoucherUseCase(get(), get(), get(), get()) }
    factory { CreateRequestVoucherUseCase(get(), get()) }
    factory { RecordCollectionUseCase(get(), get()) }
    factory { EndShiftUseCase(get()) }

    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { RouteViewModel(get(), get()) }
    viewModel { CustomerListViewModel(get()) }
    viewModel { (customerId: String) ->
        CustomerDashboardViewModel(customerId, get(), get(), get())
    }
    viewModel { (customerId: String) ->
        SaleVoucherViewModel(customerId, get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        ReturnVoucherViewModel(customerId, get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        RequestVoucherViewModel(customerId, get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        CollectionViewModel(customerId, get(), get(), get())
    }
    viewModel { (customerId: String?) ->
        AiAssistantViewModel(customerId, get(), get(), get(), get(), get())
    }
    viewModel { VanStockViewModel(get()) }
    viewModel { EndOfDayViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (customerId: String) ->
        MapNavigationViewModel(customerId, get(), get())
    }
    viewModel { (customerId: String) ->
        TransactionReportViewModel(customerId, get())
    }
    viewModel { (customerId: String) ->
        PaymentReportViewModel(customerId, get())
    }
    viewModel { (customerId: String) ->
        AccountStatementViewModel(customerId, get(), get(), get())
    }
    viewModel { (invoiceId: String) ->
        VoucherDetailViewModel(invoiceId, get(), get())
    }
    viewModel { (paymentId: String) ->
        ReceiptDetailViewModel(paymentId, get())
    }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule(), platformModule())
}
