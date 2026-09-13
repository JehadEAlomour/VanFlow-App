package com.jehadalomour.flowvan

import android.app.Application
import com.jehadalomour.flowvan.di.appFeatureModules
import com.jehadalomour.flowvan.platform.printer.androidPrinterModule
import com.jehadalomour.flowvan.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

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
        }
    }
}
