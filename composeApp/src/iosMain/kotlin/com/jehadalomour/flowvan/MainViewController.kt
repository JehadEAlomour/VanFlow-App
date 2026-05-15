package com.jehadalomour.flowvan

import androidx.compose.ui.window.ComposeUIViewController
import com.jehadalomour.flowvan.shared.di.initKoinIos

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Koin is started by Swift via initKoinIos(); calling here as a safety net
        // is harmless because startKoin no-ops if already started? — actually it throws.
    },
) { App() }

@Suppress("unused", "FunctionName") // entry point for Swift
fun StartKoin() = initKoinIos()
