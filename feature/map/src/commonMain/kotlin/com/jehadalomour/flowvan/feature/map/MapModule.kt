package com.jehadalomour.flowvan.feature.map

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun mapModule(): Module = module {
    // Two factories, one per destination kind. Koin picks by the parameter type:
    // a String is a customer id; a Point is a bare prospect location.
    viewModel { (customerId: String) ->
        MapNavigationViewModel(customerId, get(), get())
    }
    viewModel { (point: MapNavigationViewModel.Point) ->
        MapNavigationViewModel(point, get(), get())
    }
}
