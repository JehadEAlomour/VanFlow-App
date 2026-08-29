package com.jehadalomour.flowvan.feature.map

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun mapModule(): Module = module {
    // ONE definition — Koin keys viewModel by the produced TYPE, so a second
    // factory for the same type silently replaces the first (that bug made the
    // customer-location screen pass a String id into the Point factory and
    // crash with ClassCastException). Dispatch on the param instead: a Point is
    // a bare prospect location; anything else is a customer id String.
    viewModel { params ->
        val point = params.getOrNull<MapNavigationViewModel.Point>()
        if (point != null) {
            MapNavigationViewModel(point, get(), get())
        } else {
            MapNavigationViewModel(params.get<String>(), get(), get())
        }
    }
}
