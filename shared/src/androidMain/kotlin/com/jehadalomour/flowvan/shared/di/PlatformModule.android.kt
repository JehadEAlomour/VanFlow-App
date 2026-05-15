package com.jehadalomour.flowvan.shared.di

import android.content.Intent
import com.jehadalomour.flowvan.shared.data.local.db.DatabaseFactory
import com.jehadalomour.flowvan.shared.data.location.AndroidLocationProvider
import com.jehadalomour.flowvan.shared.data.location.AndroidLocationTracker
import com.jehadalomour.flowvan.shared.data.location.LocationProvider
import com.jehadalomour.flowvan.shared.data.location.LocationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TRACKING_SERVICE = "com.jehadalomour.flowvan.service.TrackingForegroundService"

actual fun platformModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<LocationTracker> {
        AndroidLocationTracker(
            context = androidContext(),
            onStartService = { ctx ->
                ctx.startForegroundService(Intent().setClassName(ctx, TRACKING_SERVICE))
            },
            onStopService = { ctx ->
                ctx.stopService(Intent().setClassName(ctx, TRACKING_SERVICE))
            },
        )
    }
}
