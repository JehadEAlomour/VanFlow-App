package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.data.device.DeviceIdentityProvider
import com.jehadalomour.flowvan.core.database.db.DatabaseFactory
import com.jehadalomour.flowvan.core.data.location.LocationStatusProvider
import com.jehadalomour.flowvan.core.data.location.SettingsOpener
import com.jehadalomour.flowvan.core.domain.notify.AlertNotifier
import com.jehadalomour.flowvan.core.domain.notify.AndroidAlertNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single { ConnectivityObserver(androidContext()) }
    single { DeviceIdentityProvider(androidContext()) }
    single { LocationStatusProvider(androidContext()) }
    single { SettingsOpener(androidContext()) }
    single<AlertNotifier> { AndroidAlertNotifier(androidContext()) }
    // LocationProvider and LocationTracker are the only bindings the two builds
    // disagree about, so they are the only ones that live in a flavour.
    includes(locationModule())
}
