package com.jehadalomour.flowvan.shared.di

import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.data.device.DeviceIdentityProvider
import com.jehadalomour.flowvan.core.database.db.DatabaseFactory
import com.jehadalomour.flowvan.core.data.location.IosLocationProvider
import com.jehadalomour.flowvan.core.data.location.IosLocationTracker
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.LocationStatusProvider
import com.jehadalomour.flowvan.core.data.location.SettingsOpener
import com.jehadalomour.flowvan.core.common.config.AppBuildConfig
import com.jehadalomour.flowvan.core.data.update.AppInstaller
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
    single { SettingsOpener() }
    single { AppInstaller() }
    single { IOS_BUILD_CONFIG }
    single<LocationProvider> { IosLocationProvider() }
    single<LocationTracker> { IosLocationTracker() }
    single<ReceiptPrinter> { IosReceiptPrinter() }
    single<AlertNotifier> { IosAlertNotifier() }
}

/**
 * iOS has no product flavours, so there is nothing to read the customer from.
 *
 * It is pinned to `dev` rather than to a customer on purpose: the iOS build is a
 * development target, and an iOS binary that quietly pointed at a live company's
 * server would be one careless `Run` away from writing real vouchers. A salesman
 * can still enter any base URL from the Settings screen, which is how the iOS
 * build is aimed at a real backend when someone means to.
 *
 * The update manifest URL is present only so the type is satisfied — iOS cannot
 * replace its own binary, and AppInstaller refuses the install there.
 */
private val IOS_BUILD_CONFIG = AppBuildConfig(
    customer = "dev",
    apiBaseUrl = "https://app-dev.7softwarejo.com/api/v1",
    updateManifestUrl = "https://7softwarejo.com/flowvan/updates/dev/ios.json",
)

fun initKoinIos() = initKoin {}
