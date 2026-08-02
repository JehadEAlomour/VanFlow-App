package com.jehadalomour.flowvan.feature.home

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun homeModule(): Module = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { RouteViewModel(get(), get()) }
    viewModel { TodayRouteViewModel(get()) }
    viewModel { EndOfDayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
