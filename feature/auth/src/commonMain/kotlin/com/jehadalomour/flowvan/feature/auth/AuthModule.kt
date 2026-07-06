package com.jehadalomour.flowvan.feature.auth

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun authModule(): Module = module {
    viewModel { LoginViewModel(get(), get()) }
}
