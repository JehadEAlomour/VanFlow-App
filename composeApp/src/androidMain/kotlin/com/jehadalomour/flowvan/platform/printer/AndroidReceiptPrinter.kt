package com.jehadalomour.flowvan.platform.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jehadalomour.flowvan.shared.domain.printer.BarcodeType
import com.jehadalomour.flowvan.shared.domain.printer.PaperWidth
import com.jehadalomour.flowvan.shared.domain.printer.PrintAlign
import com.jehadalomour.flowvan.shared.domain.printer.PrintContent
import com.jehadalomour.flowvan.shared.domain.printer.PrintNode
import com.jehadalomour.flowvan.shared.domain.printer.PrintResult
import com.jehadalomour.flowvan.shared.domain.printer.PrinterState
import com.jehadalomour.flowvan.shared.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.shared.domain.printer.PrinterType
import com.jehadalomour.flowvan.shared.domain.printer.ReceiptPrinter
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

    private var printer: POSPrinter? = null
    private var pendingTarget: PrinterTarget? = null
    private var pendingConnect: CompletableDeferred<PrintResult>? = null

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
        (state.value as? PrinterState.Connected)?.let {
            if (it.target == target && printer != null) return PrintResult.Success
        }
        // Drop any stale connection before opening a new one.
        runCatching { POSConnect.disconnect() }
        printer = null

        val deferred = CompletableDeferred<PrintResult>()
        pendingConnect = deferred
        pendingTarget = target
        _state.value = PrinterState.Connecting(target)

        val launched = withContext(Dispatchers.IO) {
            runCatching {
                when (target.type) {
                    PrinterType.USB -> POSConnect.connectUSB(target.address)
                    PrinterType.BLUETOOTH -> POSConnect.connectBT(target.address)
                    PrinterType.NETWORK -> POSConnect.connectNet(target.address)
                    PrinterType.SERIAL -> POSConnect.connectSerial(target.address, target.baudRate.toString())
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

    override fun disconnect() {
        runCatching { POSConnect.disconnect() }
        printer = null
        _state.value = PrinterState.Disconnected
    }

    override suspend fun print(content: PrintContent): PrintResult {
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
        paperWidth: PaperWidth,
        align: PrintAlign,
        cut: Boolean,
    ): PrintResult {
        val p = ensureReady() ?: return notConnected()
        return withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
                    ?: error("صورة غير صالحة")
                p.initializePrinter()
                    .printBitmap(bitmap, align.toSdk(), paperWidth.dots)
                    .feedLine(3)
                if (cut) p.cutPaper(POSConst.CUT_HALF)
            }.fold(
                onSuccess = { PrintResult.Success },
                onFailure = { PrintResult.Failure(it.message ?: "فشلت طباعة الصورة") },
            )
        }
    }

    override suspend fun openCashDrawer(): PrintResult {
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
            .apply()
    }

    private fun loadLastTarget(): PrinterTarget? {
        val type = prefs.getString(KEY_TYPE, null) ?: return null
        val address = prefs.getString(KEY_ADDRESS, null) ?: return null
        return PrinterTarget(
            type = runCatching { PrinterType.valueOf(type) }.getOrNull() ?: return null,
            address = address,
            name = prefs.getString(KEY_NAME, address) ?: address,
            baudRate = prefs.getInt(KEY_BAUD, 115200),
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val KEY_TYPE = "type"
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
        const val KEY_BAUD = "baud"
    }
}
