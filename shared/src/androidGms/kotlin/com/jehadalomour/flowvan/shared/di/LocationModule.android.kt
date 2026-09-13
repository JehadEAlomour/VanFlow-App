package com.jehadalomour.flowvan.shared.di

import android.content.Intent
import com.jehadalomour.flowvan.core.data.location.AndroidLocationProvider
import com.jehadalomour.flowvan.core.data.location.AndroidLocationTracker
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.LocationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Referenced by name rather than by class so :shared does not have to depend on
 * :composeApp, which depends on it. The service is declared in the `gms`
 * manifest only — which is fine, because this file is only compiled into that
 * build.
 */
private const val TRACKING_SERVICE = "com.jehadalomour.flowvan.service.TrackingForegroundService"

/** Real location: fused fixes from Play Services, uploaded by a foreground service. */
internal fun locationModule(): Module = module {
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
