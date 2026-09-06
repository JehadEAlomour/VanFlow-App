package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import com.jehadalomour.flowvan.core.model.InvoiceAppliedOffer
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.network.api.VoucherApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class BulkPhase { LOADING, EMPTY, NEED_CONNECT, PRINTING, DONE, ERROR }

/**
 * Bulk "print every invoice's detail (with the tax QR)" for the sales report.
 *
 * The thermal printer prints one image at a time, so this walks the range's SALE
 * invoices sequentially: build one invoice's receipt state → the screen captures it
 * to a PNG and hands it back → print → advance. The receipt is the same
 * [ReceiptBody] the single-invoice screen prints, so the JoFotara QR rides along.
 */
data class SalesBulkPrintState(
    val phase: BulkPhase = BulkPhase.LOADING,
    val total: Int = 0,
    val printedCount: Int = 0,
    /** The invoice currently being rendered for capture; null when idle/done. */
    val current: VoucherPrintState? = null,
    /** Bumped to ask the screen to capture [current]; the screen echoes it back. */
    val captureNonce: Int = 0,
    val printerState: PrinterState = PrinterState.Disconnected,
    val message: String? = null,
    val showConnectDialog: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
)

sealed interface SalesBulkPrintEvent {
    data object RequestConnect : SalesBulkPrintEvent
    data object DismissConnectDialog : SalesBulkPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : SalesBulkPrintEvent
    data class ConnectAddressChanged(val address: String) : SalesBulkPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : SalesBulkPrintEvent
    data object RefreshDevices : SalesBulkPrintEvent
    data object Connect : SalesBulkPrintEvent
    /** The screen captured [SalesBulkPrintState.current] as a PNG at [nonce]. */
    data class PngCaptured(val png: ByteArray, val nonce: Int) : SalesBulkPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is PngCaptured && nonce == other.nonce && png.contentEquals(other.png))
        override fun hashCode() = 31 * nonce + png.contentHashCode()
    }
    data object Retry : SalesBulkPrintEvent
    data object DismissMessage : SalesBulkPrintEvent
}

class SalesBulkPrintViewModel(
    private val fromMillis: Long,
    private val toMillis: Long,
    private val invoiceDao: InvoiceDao,
    private val customers: CustomerRepository,
    private val users: UserRepository,
    private val appSettings: AppSettingsRepository,
    private val companyInfo: CompanyInfoRepository,
    private val products: ProductRepository,
    private val json: Json,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
    private val voucherApi: VoucherApi,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SalesBulkPrintState(
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<SalesBulkPrintState> = _state.asStateFlow()

    private var ids: List<String> = emptyList()
    private var index = 0
    private var nonce = 0
    private var companyNameAr = ""
    private var companyNameEn = ""
    private var companyTaxNumber = ""
    private var companyLogo = ""
    private var companyPhone = ""

    init {
        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val info = companyInfo.getForPrint()
            companyNameAr = info.nameAr
            companyNameEn = info.nameEn
            companyTaxNumber = info.taxNumber
            companyLogo = info.logo
            companyPhone = info.phone

            val all = invoiceDao.observeAllByRange(fromMillis, toMillis).first()
            ids = all.filter { it.type == "SALE" }.map { it.id }
            _state.update { it.copy(total = ids.size) }

            if (ids.isEmpty()) {
                _state.update { it.copy(phase = BulkPhase.EMPTY) }
                return@launch
            }
            if (_state.value.printerState is PrinterState.Connected) {
                startPrinting()
            } else {
                _state.update { it.copy(phase = BulkPhase.NEED_CONNECT, showConnectDialog = true) }
                refreshDevices()
            }
        }
    }

    fun onEvent(event: SalesBulkPrintEvent) {
        when (event) {
            SalesBulkPrintEvent.RequestConnect -> {
                _state.update { it.copy(showConnectDialog = true, message = null) }
                refreshDevices()
            }
            SalesBulkPrintEvent.DismissConnectDialog -> _state.update { it.copy(showConnectDialog = false) }
            is SalesBulkPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }
            is SalesBulkPrintEvent.ConnectAddressChanged -> _state.update { it.copy(connectAddress = event.address) }
            is SalesBulkPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }
            SalesBulkPrintEvent.RefreshDevices -> refreshDevices()
            SalesBulkPrintEvent.Connect -> connect()
            is SalesBulkPrintEvent.PngCaptured -> onPng(event.png, event.nonce)
            SalesBulkPrintEvent.Retry -> {
                if (ids.isNotEmpty() && index < ids.size) {
                    _state.update { it.copy(phase = BulkPhase.PRINTING, message = null) }
                    emitCurrent()
                }
            }
            SalesBulkPrintEvent.DismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun refreshDevices() {
        val devices = when (_state.value.connectType) {
            PrinterType.BLUETOOTH -> printer.discoverBluetooth()
            PrinterType.USB -> printer.discoverUsb()
            PrinterType.SERIAL -> printer.discoverSerialPorts()
            PrinterType.NETWORK -> emptyList()
        }
        _state.update { it.copy(discoveredDevices = devices) }
    }

    private fun connect() {
        val s = _state.value
        if (s.connectAddress.isBlank()) return
        val target = PrinterTarget(
            type = s.connectType,
            address = s.connectAddress,
            name = s.connectAddress,
            baudRate = printer.lastTarget?.baudRate ?: 115200,
        )
        viewModelScope.launch {
            when (val result = printer.connect(target)) {
                is PrintResult.Success -> {
                    _state.update { it.copy(showConnectDialog = false) }
                    startPrinting()
                }
                is PrintResult.Failure -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    private fun startPrinting() {
        index = 0
        _state.update { it.copy(phase = BulkPhase.PRINTING, showConnectDialog = false, printedCount = 0) }
        emitCurrent()
    }

    /** Build the current invoice's receipt state and ask the screen to capture it. */
    private fun emitCurrent() {
        val id = ids.getOrNull(index)
        if (id == null) {
            _state.update { it.copy(phase = BulkPhase.DONE, current = null) }
            return
        }
        viewModelScope.launch {
            val entity = runCatching { invoiceDao.observeById(id).first() }.getOrNull()
            if (entity == null) { advance(); return@launch }
            val st = buildState(entity)
            nonce += 1
            _state.update { it.copy(current = st, captureNonce = nonce) }
        }
    }

    private fun onPng(png: ByteArray, forNonce: Int) {
        if (forNonce != nonce) return // stale capture from a previous invoice
        if (_state.value.printerState !is PrinterState.Connected) {
            _state.update { it.copy(phase = BulkPhase.NEED_CONNECT, showConnectDialog = true) }
            return
        }
        viewModelScope.launch {
            when (val result = printer.printImage(png, PaperWidth.MM80)) {
                is PrintResult.Success -> {
                    _state.update { it.copy(printedCount = it.printedCount + 1) }
                    advance()
                }
                is PrintResult.Failure ->
                    _state.update { it.copy(phase = BulkPhase.ERROR, message = result.message) }
            }
        }
    }

    private fun advance() {
        index += 1
        if (index >= ids.size) {
            _state.update { it.copy(phase = BulkPhase.DONE, current = null) }
        } else {
            emitCurrent()
        }
    }

    private suspend fun buildState(entity: com.jehadalomour.flowvan.core.database.entity.InvoiceEntity): VoucherPrintState {
        val lines = runCatching {
            json.decodeFromString<List<InvoiceLine>>(entity.linesJson)
        }.getOrDefault(emptyList())
        val appliedOffers = entity.appliedOffersJson
            ?.let { runCatching { json.decodeFromString<List<InvoiceAppliedOffer>>(it) }.getOrDefault(emptyList()) }
            .orEmpty()
        val customer = customers.findById(entity.customerId)
        val salesman = users.findById(entity.salesmanId)
        val settings = appSettings.get()
        val freeLines = resolveFreeLines(entity.chosenFreeItemsCsv)

        // Prefer the cached QR; one best-effort fetch (no retry loop — this runs per
        // invoice in a bulk job) when it is missing, so a freshly-synced sale still
        // prints its QR without stalling the queue.
        var qr = entity.jofotaraQrCode
        if (qr.isNullOrBlank() && entity.type == "SALE") {
            val custNo = customer?.code
            if (!custNo.isNullOrBlank()) {
                qr = runCatching { voucherApi.saleByNumber(entity.number, custNo)?.jofotaraQrCode }.getOrNull()
                if (!qr.isNullOrBlank()) runCatching { invoiceDao.setJofotaraQr(entity.id, qr) }
            }
        }

        return VoucherPrintState(
            isLoading = false,
            canPrintLineDiscount = session.canPrintLineDiscount,
            invoiceId = entity.id,
            number = entity.number,
            type = entity.type,
            paymentMethod = entity.paymentMethod,
            createdAt = entity.createdAt,
            customerNameAr = customer?.nameAr.orEmpty(),
            customerCode = customer?.code.orEmpty(),
            customerTaxNumber = customer?.taxNumber,
            salesmanNameAr = salesman?.nameAr.orEmpty(),
            lines = lines,
            freeLines = freeLines,
            appliedOffers = appliedOffers,
            subtotal = entity.subtotal,
            discountAmount = entity.discountAmount,
            taxAmount = entity.taxAmount,
            total = entity.total,
            notes = entity.notes,
            isTaxExempt = entity.isTaxExempt,
            taxExemptionNumber = entity.taxExemptionNumber,
            branch = settings.branch,
            qrData = qr,
            companyNameAr = companyNameAr,
            companyNameEn = companyNameEn,
            companyTaxNumber = companyTaxNumber,
            companyLogo = companyLogo,
            companyPhone = companyPhone,
            printerState = _state.value.printerState,
        )
    }

    /** Mirror of VoucherPrintViewModel.resolveFreeLines — gifts print as normal 100%-off lines. */
    private suspend fun resolveFreeLines(csv: String?): List<InvoiceLine> {
        val counts = csv?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.groupingBy { it }
            ?.eachCount()
            .orEmpty()
        return counts.map { (sku, qty) ->
            val product = products.findBySku(sku)
            InvoiceLine(
                productId = product?.id.orEmpty(),
                sku = sku,
                nameAr = product?.nameAr ?: sku,
                qty = qty.toDouble(),
                unitPrice = product?.salePrice ?: 0.0,
                discountPct = 1.0,
                lineTotal = 0.0,
                taxType = "EXEMPT",
                taxAmount = 0.0,
                unit = product?.unit.orEmpty(),
                taxRate = 0.0,
            )
        }
    }
}
