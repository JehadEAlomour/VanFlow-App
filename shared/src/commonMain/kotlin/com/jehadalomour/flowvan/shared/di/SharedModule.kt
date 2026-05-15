package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.shared.data.local.db.FlowVanDatabase
import com.jehadalomour.flowvan.shared.data.local.db.buildFlowVanDatabase
import com.jehadalomour.flowvan.shared.data.repository.UserRepository
import com.jehadalomour.flowvan.shared.data.seeder.DemoSeeder
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LoginUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LogoutUseCase
import com.jehadalomour.flowvan.shared.presentation.feature.login.LoginViewModel
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
    single { DemoSeeder(get(), get(), get()) }

    factory { LoginUseCase(get(), get()) }
    factory { GetCurrentUserUseCase(get(), get()) }
    factory { LogoutUseCase(get()) }

    viewModel { LoginViewModel(get(), get()) }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule(), platformModule())
}
