package com.jehadalomour.flowvan

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.jehadalomour.flowvan.platform.printer.androidPrinterModule
import com.jehadalomour.flowvan.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class FlowVanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, null)
        initKoin {
            androidLogger(Level.INFO)
            androidContext(this@FlowVanApp)
            // XPrinter-backed ReceiptPrinter lives in :composeApp (local .aar); bind it here.
            modules(androidPrinterModule())
        }
    }
}
