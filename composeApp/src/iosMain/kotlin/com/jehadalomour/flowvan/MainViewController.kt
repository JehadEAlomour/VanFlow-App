package com.jehadalomour.flowvan

import androidx.compose.ui.window.ComposeUIViewController
import com.jehadalomour.flowvan.di.appFeatureModules
import com.jehadalomour.flowvan.shared.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Koin is started by Swift via StartKoin(); calling here as a safety net
        // is harmless because startKoin no-ops if already started? — actually it throws.
    },
) { App() }

@Suppress("unused", "FunctionName") // entry point for Swift
fun StartKoin() = initKoin { modules(appFeatureModules()) }
