package com.jehadalomour.flowvan.core.domain.printer

import kotlinx.coroutines.flow.StateFlow

/** How the device is wired to the printer. */
enum class PrinterType { USB, BLUETOOTH, SERIAL, NETWORK }

/**
 * The command language the printer speaks. ESC/POS is the default (the XPrinter/POS
 * thermal printers); CPCL is for Zebra mobile printers, which the ESC/POS SDK cannot
 * drive — the admin picks it when the connected printer is a Zebra/CPCL model, and
 * the app routes printing through the Zebra SDK instead.
 */
enum class PrinterLanguage { ESCPOS, CPCL }

/**
 * A connectable printer.
 *
 * - [address] meaning depends on [type]: USB device path, Bluetooth MAC, serial port path,
 *   or `host[:port]` for network.
 * - [baudRate] only applies to [PrinterType.SERIAL].
 * - [language] selects the command set / SDK; Zebra CPCL printers connect over Bluetooth.
 */
data class PrinterTarget(
    val type: PrinterType,
    val address: String,
    val name: String = address,
    val baudRate: Int = 115200,
    val language: PrinterLanguage = PrinterLanguage.ESCPOS,
)

/** Live connection state, observable from a ViewModel. */
sealed interface PrinterState {
    data object Disconnected : PrinterState
    data class Connecting(val target: PrinterTarget) : PrinterState
    data class Connected(val target: PrinterTarget) : PrinterState
    data class Error(val message: String) : PrinterState
}

/** Outcome of a connect / print call. */
sealed interface PrintResult {
    data object Success : PrintResult
    data class Failure(val message: String) : PrintResult

    val isSuccess: Boolean get() = this is Success
}

/**
 * Single entry point for all physical printing in the app.
 *
 * A process-wide singleton (provided as a Koin `single`): the connection it holds survives
 * navigation, so connect once and every later call reuses it. ViewModels depend on this
 * interface; the platform implementation lives in the app module (the XPrinter SDK is a local
 * `.aar`, which cannot live in the `:shared` library module). On iOS this is a no-op stub.
 */
interface ReceiptPrinter {

    val state: StateFlow<PrinterState>

    /** The last target we attempted to connect to, restored across app launches. */
    val lastTarget: PrinterTarget?

    /**
     * The command language this device's printer speaks — a persistent, device-wide
     * setting the admin chooses once (ESC/POS vs a Zebra CPCL printer). Applied to
     * every [connect] so all print screens route to the right SDK without each having
     * to ask again. Persisted across launches.
     */
    var language: PrinterLanguage

    /** Open a connection. Safe to call when already connected to the same target. */
    suspend fun connect(target: PrinterTarget): PrintResult

    fun disconnect()

    /** Print structured content. Fails fast if not connected. */
    suspend fun print(content: PrintContent): PrintResult

    /**
     * Print a PNG image scaled to the paper width — the recommended path for Arabic receipts,
     * since printer firmware cannot shape Arabic glyphs.
     */
    suspend fun printImage(
        png: ByteArray,
        paperWidth: PaperWidth = PaperWidth.MM80,
        align: PrintAlign = PrintAlign.CENTER,
        cut: Boolean = true,
    ): PrintResult

    suspend fun openCashDrawer(): PrintResult

    /** Paired (bonded) Bluetooth printers. Requires the Bluetooth runtime permission. */
    fun discoverBluetooth(): List<PrinterTarget>

    /** Currently attached USB printers. */
    fun discoverUsb(): List<PrinterTarget>

    /** Available on-board serial ports (built-in printers on handheld POS units). */
    fun discoverSerialPorts(): List<PrinterTarget>
}
