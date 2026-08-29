package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.data.device.DeviceIdentityProvider
import com.jehadalomour.flowvan.core.database.db.DatabaseFactory
import com.jehadalomour.flowvan.core.data.location.IosLocationProvider
import com.jehadalomour.flowvan.core.data.location.IosLocationTracker
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.LocationStatusProvider
import com.jehadalomour.flowvan.core.data.location.LocationTracker
import com.jehadalomour.flowvan.core.domain.printer.IosReceiptPrinter
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import com.jehadalomour.flowvan.core.domain.notify.AlertNotifier
import com.jehadalomour.flowvan.core.domain.notify.IosAlertNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
    single { ConnectivityObserver() }
    single { DeviceIdentityProvider() }
    single { LocationStatusProvider() }
    single<LocationProvider> { IosLocationProvider() }
    single<LocationTracker> { IosLocationTracker() }
    single<ReceiptPrinter> { IosReceiptPrinter() }
    single<AlertNotifier> { IosAlertNotifier() }
}

fun initKoinIos() = initKoin {}
