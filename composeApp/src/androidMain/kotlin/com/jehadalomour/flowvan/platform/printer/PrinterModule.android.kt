package com.jehadalomour.flowvan.platform.printer

import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the XPrinter-backed [ReceiptPrinter] for Android. Loaded from `FlowVanApp` because the
 * XPrinter `.aar` lives in `:composeApp`, not the `:shared` library module.
 */
fun androidPrinterModule(): Module = module {
    single<ReceiptPrinter> { AndroidReceiptPrinter(androidContext()) }
}
