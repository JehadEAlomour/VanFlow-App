package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.LocationTracker
import com.jehadalomour.flowvan.core.data.location.NoLocationProvider
import com.jehadalomour.flowvan.core.data.location.NoLocationTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * No location: the same two interfaces, bound to implementations that produce
 * nothing and start nothing. There is no foreground service to name here — the
 * `nogms` build does not ship one.
 */
internal fun locationModule(): Module = module {
    single<LocationProvider> { NoLocationProvider() }
    single<LocationTracker> { NoLocationTracker() }
}
