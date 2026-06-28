package com.jehadalomour.flowvan.feature.map

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun mapModule(): Module = module {
    viewModel { (customerId: String) ->
        MapNavigationViewModel(customerId, get(), get())
    }
}
