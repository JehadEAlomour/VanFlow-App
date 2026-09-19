package com.jehadalomour.flowvan

import android.app.Application
import com.jehadalomour.flowvan.core.common.config.AppBuildConfig
import com.jehadalomour.flowvan.di.appFeatureModules
import com.jehadalomour.flowvan.platform.printer.androidPrinterModule
import com.jehadalomour.flowvan.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.dsl.module

class FlowVanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Google Maps needs warming up before the first map screen; the `nogms`
        // build has no Maps SDK to warm, so this is a no-op there.
        initPlatformServices()
        initKoin {
            androidLogger(Level.INFO)
            androidContext(this@FlowVanApp)
            // XPrinter-backed ReceiptPrinter lives in :composeApp (local .aar); bind it here.
            modules(androidPrinterModule())
            // Feature ViewModels (one Koin module per :feature:*).
            modules(appFeatureModules())
            // Which customer this APK is for, and the two URLs that follow from
            // it. Bound HERE rather than in :shared because BuildConfig is
            // generated into the application module, which :shared is a
            // dependency of and therefore cannot see.
            modules(
                module {
                    single {
                        AppBuildConfig(
                            customer = BuildConfig.CUSTOMER,
                            apiBaseUrl = BuildConfig.API_BASE_URL,
                            updateManifestUrl = BuildConfig.UPDATE_MANIFEST_URL,
                        )
                    }
                },
            )
        }
    }
}
