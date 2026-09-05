package com.jehadalomour.flowvan.core.domain.printer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS stub. The XPrinter SDK is Android-only (JNI serial driver), so printing is unsupported
 * here. Calls fail gracefully; wire up a CoreBluetooth / `Network.framework` ESC/POS path later.
 */
class IosReceiptPrinter : ReceiptPrinter {
    private val _state = MutableStateFlow<PrinterState>(PrinterState.Disconnected)
    override val state: StateFlow<PrinterState> = _state
    override val lastTarget: PrinterTarget? = null
    override var language: PrinterLanguage = PrinterLanguage.ESCPOS

    private val unsupported = PrintResult.Failure("الطباعة غير مدعومة على iOS بعد")

    override suspend fun connect(target: PrinterTarget): PrintResult = unsupported
    override fun disconnect() {}
    override suspend fun print(content: PrintContent): PrintResult = unsupported
    override suspend fun printImage(
        png: ByteArray,
        paperWidth: PaperWidth,
        align: PrintAlign,
        cut: Boolean,
    ): PrintResult = unsupported
    override suspend fun openCashDrawer(): PrintResult = unsupported
    override fun discoverBluetooth(): List<PrinterTarget> = emptyList()
    override fun discoverUsb(): List<PrinterTarget> = emptyList()
    override fun discoverSerialPorts(): List<PrinterTarget> = emptyList()
}
