package com.jehadalomour.flowvan.platform.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jehadalomour.flowvan.core.domain.printer.BarcodeType
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintAlign
import com.jehadalomour.flowvan.core.domain.printer.PrintContent
import com.jehadalomour.flowvan.core.domain.printer.PrintNode
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.posprinter.POSConnect
import net.posprinter.POSConst
import net.posprinter.POSPrinter

/**
 * Android [ReceiptPrinter] backed by the XPrinter (`net.posprinter`) SDK.
 *
 * Wraps the SDK's single global connection in a coroutine-friendly, observable form.
 * The SDK reports connect results asynchronously through one global listener, which we bridge
 * back to [connect]'s suspending call and to [state]. Provided as a Koin `single`, so the held
 * connection survives navigation.
 *
 * Uses the SDK's static [POSConnect] API. It is marked deprecated in favour of the per-device
 * instance API, but that path drives several printers at once and cannot pass a serial baud rate
 * — neither of which we need. A single shared connection is exactly our model.
 */
@Suppress("DEPRECATION")
class AndroidReceiptPrinter(appContext: Context) : ReceiptPrinter {

    private val context = appContext.applicationContext
    private val prefs = appContext.getSharedPreferences("flowvan_printer", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<PrinterState>(PrinterState.Disconnected)
    override val state: StateFlow<PrinterState> = _state.asStateFlow()

    override var lastTarget: PrinterTarget? = loadLastTarget()
        private set

    // Device-wide printer language (ESC/POS vs Zebra CPCL). Persisted; the setter
    // writes through so the admin's choice survives an app restart.
    override var language: PrinterLanguage = loadLanguage()
        set(value) {
            field = value
            prefs.edit().putString(KEY_LANG, value.name).apply()
        }

    // The roll this head actually covers. Persisted like the language, and for the
    // same reason: it is a property of the device, chosen once.
    override var paperWidth: PaperWidth = loadPaperWidth()
        set(value) {
            field = value
            prefs.edit().putString(KEY_PAPER, value.name).apply()
        }

    private var printer: POSPrinter? = null
    private var pendingTarget: PrinterTarget? = null
    private var pendingConnect: CompletableDeferred<PrintResult>? = null

    // CPCL (Zebra) backend, used when the connected target's language is CPCL. The two
    // backends are mutually exclusive — only one is connected at a time.
    private val zebra = ZebraCpclPrinter()

    private fun connectedTarget(): PrinterTarget? =
        (state.value as? PrinterState.Connected)?.target
    private fun isCpcl(): Boolean = connectedTarget()?.language == PrinterLanguage.CPCL
    private fun isReady(target: PrinterTarget): Boolean =
        if (target.language == PrinterLanguage.CPCL) zebra.isConnected else printer != null

    init {
        POSConnect.init(appContext.applicationContext) { code, _ -> onSdkStatus(code) }
    }

    private fun onSdkStatus(code: Int) {
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                val connection = POSConnect.getConnect()
                val target = pendingTarget
                if (connection != null && target != null) {
                    printer = POSPrinter(connection)
                    _state.value = PrinterState.Connected(target)
                    saveLastTarget(target)
                    pendingConnect?.complete(PrintResult.Success)
                } else {
                    pendingConnect?.complete(PrintResult.Failure("لم يتم تهيئة الطابعة"))
                }
            }

            POSConnect.CONNECT_FAIL,
            POSConnect.SEND_FAIL -> {
                printer = null
                _state.value = PrinterState.Error("فشل الاتصال بالطابعة")
                pendingConnect?.complete(PrintResult.Failure("فشل الاتصال بالطابعة"))
            }

            POSConnect.CONNECT_INTERRUPT,
            POSConnect.USB_DETACHED,
            POSConnect.BLUETOOTH_INTERRUPT -> {
                printer = null
                _state.value = PrinterState.Disconnected
            }
        }
        pendingConnect = null
    }

    override suspend fun connect(target: PrinterTarget): PrintResult {
        // The device-wide language is the source of truth — stamp it onto the target
        // so the connected state, routing, and persistence all agree, whatever the
        // caller passed.
        val effective = target.copy(language = language)
        (state.value as? PrinterState.Connected)?.let {
            if (it.target == effective && isReady(effective)) return PrintResult.Success
        }
        // Switching printer / language — tear down BOTH backends before opening a new one.
        runCatching { POSConnect.disconnect() }
        printer = null
        zebra.close()

        // A CPCL printer is driven by the Zebra SDK, not the ESC/POS one.
        if (effective.language == PrinterLanguage.CPCL) return connectZebra(effective)

        val deferred = CompletableDeferred<PrintResult>()
        pendingConnect = deferred
        pendingTarget = effective
        _state.value = PrinterState.Connecting(effective)

        val launched = withContext(Dispatchers.IO) {
            runCatching {
                when (effective.type) {
                    PrinterType.USB -> POSConnect.connectUSB(effective.address)
                    PrinterType.BLUETOOTH -> POSConnect.connectBT(effective.address)
                    PrinterType.NETWORK -> POSConnect.connectNet(effective.address)
                    PrinterType.SERIAL -> POSConnect.connectSerial(effective.address, effective.baudRate.toString())
                }
            }
        }
        if (launched.isFailure) {
            pendingConnect = null
            val msg = launched.exceptionOrNull()?.message ?: "تعذر بدء الاتصال"
            _state.value = PrinterState.Error(msg)
            return PrintResult.Failure(msg)
        }

        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { deferred.await() } ?: run {
            pendingConnect = null
            _state.value = PrinterState.Error("انتهت مهلة الاتصال بالطابعة")
            PrintResult.Failure("انتهت مهلة الاتصال بالطابعة")
        }
    }

    /** Open a Zebra CPCL connection (Bluetooth). Synchronous SDK call, off the main thread. */
    private suspend fun connectZebra(target: PrinterTarget): PrintResult {
        _state.value = PrinterState.Connecting(target)
        return withContext(Dispatchers.IO) {
            runCatching { zebra.connect(target.address) }.fold(
                onSuccess = {
                    _state.value = PrinterState.Connected(target)
                    saveLastTarget(target)
                    PrintResult.Success
                },
                onFailure = {
                    val msg = it.message ?: "فشل الاتصال بطابعة CPCL"
                    _state.value = PrinterState.Error(msg)
                    PrintResult.Failure(msg)
                },
            )
        }
    }

    override fun disconnect() {
        runCatching { POSConnect.disconnect() }
        printer = null
        zebra.close()
        _state.value = PrinterState.Disconnected
    }

    override suspend fun print(content: PrintContent): PrintResult {
        // CPCL receipts are always rendered to a bitmap and sent via printImage (Arabic
        // can't be shaped by firmware), so the structured node path is ESC/POS-only.
        if (isCpcl()) return PrintResult.Failure("طباعة CPCL تتم عبر الصورة")
        val p = ensureReady() ?: return notConnected()
        return withContext(Dispatchers.IO) {
            runCatching {
                p.initializePrinter()
                content.nodes.forEach { node -> p.render(node, content.paperWidth) }
            }.fold(
                onSuccess = { PrintResult.Success },
                onFailure = { PrintResult.Failure(it.message ?: "فشلت الطباعة") },
            )
        }
    }

    override suspend fun printImage(
        png: ByteArray,
        paperWidth: PaperWidth?,
        align: PrintAlign,
        cut: Boolean,
    ): PrintResult {
        // Null means "whatever this printer is set to", which is what every caller
        // wants: the head's width is a fact about the device, not about the document.
        val dotsFor = paperWidth ?: this.paperWidth
        // CPCL (Zebra) path — the GraphicsUtil renders the same PNG to the printer's
        // language. No paper cut: mobile CPCL printers tear off, they don't cut.
        if (isCpcl()) {
            if (!zebra.isConnected) return notConnected()
            return withContext(Dispatchers.IO) {
                runCatching { zebra.printImage(png) }.fold(
                    onSuccess = { PrintResult.Success },
                    onFailure = { PrintResult.Failure(it.message ?: "فشلت طباعة الصورة") },
                )
            }
        }
        val p = ensureReady() ?: return notConnected()
        return withContext(Dispatchers.IO) {
            runCatching {
                val decoded = BitmapFactory.decodeByteArray(png, 0, png.size)
                    ?: error("صورة غير صالحة")
                // Fit to the head, then send the dots ourselves.
                //
                // The bitmap arrives at the capture's dp width times the
                // SCREEN's density, so its pixel width is whatever the device
                // happens to produce. Shrink it when it is wider than the head;
                // never enlarge it when it is narrower, because enlarging adds
                // no detail and only smears each glyph over more dots — a
                // narrow capture prints one pixel to one dot on a slightly
                // narrower column of paper, which is crisp, and correct.
                //
                // From there [EscPosRaster] takes over: threshold, pack, send.
                // The SDK's own printBitmap is not in this path any more — see
                // that file for why.
                val target = printTargetWidth(decoded.width, dotsFor.dots)
                val bitmap = fitWidth(decoded, target)

                val payload = ArrayList<ByteArray>()
                payload += EscPosRaster.init()
                payload += EscPosRaster.align(align.toEscPos())
                payload += EscPosRaster.rasterCommands(bitmap)
                payload += EscPosRaster.feed(3)
                if (cut) payload += EscPosRaster.cut()
                // One call, in order. The SDK writes the list to the connection
                // as a single stream, which is what keeps the slices abutting.
                p.sendData(payload)
                bitmap.recycle()
            }.fold(
                onSuccess = { PrintResult.Success },
                onFailure = { PrintResult.Failure(it.message ?: "فشلت طباعة الصورة") },
            )
        }
    }

    /**
     * The image at exactly [dots] wide, height scaled to match.
     *
     * Already-correct bitmaps are returned untouched — the common case on a phone
     * whose density happens to land on the right number, and there is no reason to
     * copy a bitmap to change nothing.
     */
    /**
     * The width to print at: never wider than the head, never wider than the
     * capture itself, and always a whole number of bytes.
     *
     * Rounding DOWN to the multiple of 8 matters — rounding up would either
     * overrun the head or re-introduce the enlargement this avoids.
     */
    private fun printTargetWidth(sourceWidth: Int, dots: Int): Int =
        (minOf(sourceWidth, dots) / 8 * 8).coerceAtLeast(8)

    /**
     * Resize to [target] only when the source is wider. Bilinear, deliberately:
     * shrinking a 1-pixel stroke with nearest-neighbour drops it outright on the
     * rows the sampling happens to miss, leaving letters with holes in them.
     * Blending keeps the stroke's weight as grey, and [toMonochrome] — which runs
     * after this — turns it back into a solid dot.
     */
    private fun fitWidth(src: Bitmap, target: Int): Bitmap {
        if (src.width <= target) return src
        val height = (src.height.toLong() * target / src.width).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(src, target, height, true)
        if (out !== src) src.recycle()
        return out
    }

    /**
     * Every pixel forced to pure black or pure white.
     *
     * The threshold is deliberately generous — anything below mid-grey burns —
     * because a thin Arabic stroke is largely edge pixels, and a strict threshold
     * erodes the letter until it is unreadable. Luminance is the usual weighted
     * mix; a plain average turns red and blue text to mud.
     */
    private fun toMonochrome(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            // Transparent is paper, not ink — a captured corner radius is a hole,
            // and treating it as black would print a solid block.
            val lum = if (a < 0x80) 0xFF else (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (lum < 0x80) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (out !== src) src.recycle()
        return out
    }

    override suspend fun openCashDrawer(): PrintResult {
        // Mobile CPCL printers have no cash drawer — treat as a no-op success so a
        // field flow that calls this doesn't surface an error.
        if (isCpcl()) return PrintResult.Success
        val p = ensureReady() ?: return notConnected()
        return withContext(Dispatchers.IO) {
            runCatching { p.openCashBox(POSConst.PIN_TWO) }.fold(
                onSuccess = { PrintResult.Success },
                onFailure = { PrintResult.Failure(it.message ?: "تعذر فتح الدرج") },
            )
        }
    }

    @SuppressLint("MissingPermission") // guarded by runCatching; permission requested in MainActivity
    override fun discoverBluetooth(): List<PrinterTarget> {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return emptyList()
        return runCatching {
            adapter.bondedDevices.orEmpty().map { device ->
                PrinterTarget(PrinterType.BLUETOOTH, device.address, name = device.name ?: device.address)
            }
        }.getOrDefault(emptyList())
    }

    override fun discoverUsb(): List<PrinterTarget> =
        runCatching { POSConnect.getUsbDevices(POSConnect.getAppCtx()) }
            .getOrDefault(emptyList())
            .map { PrinterTarget(PrinterType.USB, it, name = it.substringAfterLast('/')) }

    override fun discoverSerialPorts(): List<PrinterTarget> =
        runCatching { POSConnect.getSerialPort() }
            .getOrDefault(emptyList())
            .map { PrinterTarget(PrinterType.SERIAL, it, name = it.substringAfterLast('/')) }

    // ── internals ─────────────────────────────────────────────────────────────

    private fun ensureReady(): POSPrinter? =
        printer?.takeIf { state.value is PrinterState.Connected }

    private fun notConnected() = PrintResult.Failure("الطابعة غير متصلة")

    // ESC a n, the alignment the raster path uses. Same three values as the
    // SDK's own constants happen to be, but named against the spec rather than
    // against a jar we no longer trust to agree with it.
    private fun PrintAlign.toEscPos(): Int = when (this) {
        PrintAlign.LEFT -> 0
        PrintAlign.CENTER -> 1
        PrintAlign.RIGHT -> 2
    }

    private fun PrintAlign.toSdk(): Int = when (this) {
        PrintAlign.LEFT -> POSConst.ALIGNMENT_LEFT
        PrintAlign.CENTER -> POSConst.ALIGNMENT_CENTER
        PrintAlign.RIGHT -> POSConst.ALIGNMENT_RIGHT
    }

    private fun BarcodeType.toSdk(): Int = when (this) {
        BarcodeType.UPCA -> POSConst.BCS_UPCA
        BarcodeType.UPCE -> POSConst.BCS_UPCE
        BarcodeType.EAN13 -> POSConst.BCS_EAN13
        BarcodeType.EAN8 -> POSConst.BCS_EAN8
        BarcodeType.CODE39 -> POSConst.BCS_Code39
        BarcodeType.ITF -> POSConst.BCS_ITF
        BarcodeType.CODABAR -> POSConst.BCS_Codabar
        BarcodeType.CODE93 -> POSConst.BCS_Code93
        BarcodeType.CODE128 -> POSConst.BCS_Code128
    }

    /** Translate one [PrintNode] into ESC/POS commands on [this] printer. */
    private fun POSPrinter.render(node: PrintNode, paper: PaperWidth) {
        when (node) {
            is PrintNode.Text -> {
                var attr = 0
                if (node.bold) attr = attr or POSConst.FNT_BOLD
                if (node.underline) attr = attr or POSConst.FNT_UNDERLINE
                val w = node.widthScale.coerceIn(1, 8)
                val h = node.heightScale.coerceIn(1, 8)
                val size = ((w - 1) shl 4) or (h - 1)
                printText(node.text + "\n", node.align.toSdk(), attr, size)
            }

            is PrintNode.Image -> {
                val bitmap = BitmapFactory.decodeByteArray(node.png, 0, node.png.size)
                if (bitmap != null) printBitmap(bitmap, node.align.toSdk(), paper.dots)
            }

            is PrintNode.QrCode -> {
                setAlignment(node.align.toSdk())
                printQRCode(node.data, node.moduleSize)
                feedLine()
            }

            is PrintNode.Barcode -> {
                setAlignment(node.align.toSdk())
                printBarCode(node.data, node.type.toSdk())
                feedLine()
            }

            is PrintNode.Divider ->
                printText(node.char.toString().repeat(paper.charsPerLine) + "\n", POSConst.ALIGNMENT_LEFT, 0, 0)

            is PrintNode.Feed -> feedLine(node.lines.coerceAtLeast(1))

            is PrintNode.Cut -> cutPaper(if (node.partial) POSConst.CUT_HALF else POSConst.CUT_ALL)

            PrintNode.OpenCashDrawer -> openCashBox(POSConst.PIN_TWO)
        }
    }

    // ── persistence of last printer ─────────────────────────────────────────────

    private fun saveLastTarget(target: PrinterTarget) {
        lastTarget = target
        prefs.edit()
            .putString(KEY_TYPE, target.type.name)
            .putString(KEY_ADDRESS, target.address)
            .putString(KEY_NAME, target.name)
            .putInt(KEY_BAUD, target.baudRate)
            .putString(KEY_LANG, target.language.name)
            .apply()
    }

    /** Defaults to 80mm — what every call site assumed before this was settable. */
    private fun loadPaperWidth(): PaperWidth =
        runCatching { PaperWidth.valueOf(prefs.getString(KEY_PAPER, "MM80") ?: "MM80") }
            .getOrDefault(PaperWidth.MM80)


    private fun loadLanguage(): PrinterLanguage =
        runCatching { PrinterLanguage.valueOf(prefs.getString(KEY_LANG, "ESCPOS") ?: "ESCPOS") }
            .getOrDefault(PrinterLanguage.ESCPOS)

    private fun loadLastTarget(): PrinterTarget? {
        val type = prefs.getString(KEY_TYPE, null) ?: return null
        val address = prefs.getString(KEY_ADDRESS, null) ?: return null
        return PrinterTarget(
            type = runCatching { PrinterType.valueOf(type) }.getOrNull() ?: return null,
            address = address,
            name = prefs.getString(KEY_NAME, address) ?: address,
            baudRate = prefs.getInt(KEY_BAUD, 115200),
            language = runCatching {
                PrinterLanguage.valueOf(prefs.getString(KEY_LANG, null) ?: "ESCPOS")
            }.getOrDefault(PrinterLanguage.ESCPOS),
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val KEY_TYPE = "type"
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
        const val KEY_BAUD = "baud"
        const val KEY_LANG = "lang"
        const val KEY_PAPER = "paper_width"

    }
}
