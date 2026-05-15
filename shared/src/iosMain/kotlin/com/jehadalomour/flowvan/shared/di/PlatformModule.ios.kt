package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.shared.data.local.db.DatabaseFactory
import com.jehadalomour.flowvan.shared.data.location.IosLocationProvider
import com.jehadalomour.flowvan.shared.data.location.LocationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
    single<LocationProvider> { IosLocationProvider() }
}

fun initKoinIos() = initKoin {}
