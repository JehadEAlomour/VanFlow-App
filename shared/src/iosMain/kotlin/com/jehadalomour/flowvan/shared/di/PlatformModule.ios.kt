package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.shared.data.local.db.DatabaseFactory
import com.jehadalomour.flowvan.shared.data.location.IosLocationProvider
import com.jehadalomour.flowvan.shared.data.location.IosLocationTracker
import com.jehadalomour.flowvan.shared.data.location.LocationProvider
import com.jehadalomour.flowvan.shared.data.location.LocationTracker
import com.jehadalomour.flowvan.shared.domain.printer.IosReceiptPrinter
import com.jehadalomour.flowvan.shared.domain.printer.ReceiptPrinter
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
    single<LocationProvider> { IosLocationProvider() }
    single<LocationTracker> { IosLocationTracker() }
    single<ReceiptPrinter> { IosReceiptPrinter() }
}

fun initKoinIos() = initKoin {}
