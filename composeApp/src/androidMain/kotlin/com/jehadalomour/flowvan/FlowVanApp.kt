package com.jehadalomour.flowvan

import android.app.Application
import com.jehadalomour.flowvan.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class FlowVanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(Level.INFO)
            androidContext(this@FlowVanApp)
        }
    }
}
