package com.jehadalomour.flowvan.platform.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.zebra.android.comm.BluetoothPrinterConnection
import com.zebra.android.comm.ZebraPrinterConnection
import com.zebra.android.printer.PrinterLanguage as ZebraLanguage
import com.zebra.android.printer.ZebraPrinter
import com.zebra.android.printer.ZebraPrinterFactory

/**
 * CPCL transport for Zebra mobile printers over Bluetooth, via the Zebra Link-OS SDK.
 *
 * Kept separate from the ESC/POS (XPrinter) path because a Zebra printer speaks
 * CPCL/ZPL, which that SDK cannot produce. Printer firmware can't shape Arabic
 * glyphs, so receipts are rendered to a bitmap (exactly as the ESC/POS path does)
 * and sent through the SDK's GraphicsUtil, which converts the image to the printer's
 * language — so the same rendered receipt prints on either printer.
 *
 * Not thread-safe: the caller (AndroidReceiptPrinter) serializes every call on
 * Dispatchers.IO and never prints concurrently. All methods throw on failure; the
 * caller maps that to a PrintResult.Failure.
 */
class ZebraCpclPrinter {
    private var connection: ZebraPrinterConnection? = null
    private var printer: ZebraPrinter? = null

    val isConnected: Boolean get() = printer != null && connection?.isConnected == true

    /** Open a Bluetooth CPCL connection to [mac]. Any prior connection is closed first. */
    fun connect(mac: String) {
        close()
        val conn = BluetoothPrinterConnection(mac)
        conn.open()
        // Force CPCL rather than auto-detect: the admin has told us this is a CPCL
        // printer, and skipping detection avoids a round trip some clones answer wrongly.
        printer = ZebraPrinterFactory.getInstance(ZebraLanguage.CPCL, conn)
        connection = conn
    }

    /**
     * Print a PNG the app already rendered to the paper width (1:1, no scaling). The
     * Zebra GraphicsUtil turns the bitmap into CPCL graphics for the connected printer.
     */
    fun printImage(png: ByteArray) {
        val p = printer ?: error("Zebra printer not connected")
        val bmp: Bitmap =
            BitmapFactory.decodeByteArray(png, 0, png.size) ?: error("صورة غير صالحة")
        p.graphicsUtil.printImage(bmp, 0, 0, bmp.width, bmp.height, false)
    }

    fun close() {
        runCatching { connection?.close() }
        connection = null
        printer = null
    }
}
