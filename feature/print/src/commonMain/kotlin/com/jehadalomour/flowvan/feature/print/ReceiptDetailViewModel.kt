package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
    import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptDetailState(
    val entity: PaymentEntity? = null,
    val isLoading: Boolean = true,
    // ── Names for the printed receipt ───────────────────────────────────────────
    val customerNameAr: String = "",
    val customerCode: String = "",
    val salesmanNameAr: String = "",
    val companyNameAr: String = "",
    val companyNameEn: String? = null,
    /** Company logo (data:...;base64 URI) cached from /company-info; blank → bundled default. */
    val companyLogo: String = "",
    // ── Printing ──────────────────────────────────────────────────────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
    val showConnectDialog: Boolean = false,
    val pendingPrint: Boolean = false,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
)

/**
 * A single cash / cheque collection receipt — money only (no tax). Loads the
 * [PaymentEntity] and drives thermal printing of the on-screen receipt (the
 * bitmap capture stays in the UI; all printer logic lives here). Mirrors the
 * invoice [VoucherPrintViewModel] but for a payment.
 */
class ReceiptDetailViewModel(
    paymentId: String,
    paymentDao: PaymentDao,
    private val printer: ReceiptPrinter,
    private val customers: CustomerRepository,
    private val users: UserRepository,
    private val companyInfo: CompanyInfoRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ReceiptDetailState(
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<ReceiptDetailState> = _state.asStateFlow()

    init {
        paymentDao.observeById(paymentId)
            .onEach { entity ->
                if (entity == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@onEach
                }
                val customer = customers.findById(entity.customerId)
                val salesman = users.findById(entity.salesmanId)
                _state.update {
                    it.copy(
                        entity = entity,
                        isLoading = false,
                        customerNameAr = customer?.nameAr.orEmpty(),
                        customerCode = customer?.code.orEmpty(),
                        salesmanNameAr = salesman?.nameAr.orEmpty(),
                    )
                }
            }
            .launchIn(viewModelScope)

        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)

        // Company header: server-first when online, else the DB cache. Best-effort.
        viewModelScope.launch {
            val info = companyInfo.getForPrint()
            _state.update { it.copy(companyNameAr = info.nameAr, companyNameEn = info.nameEn, companyLogo = info.logo) }
        }
    }

    fun requestConnectThenPrint() {
        _state.update { it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null) }
        refreshDevices()
    }

    fun dismissConnectDialog() = _state.update { it.copy(showConnectDialog = false, pendingPrint = false) }
    fun connectTypeSelected(type: PrinterType) { _state.update { it.copy(connectType = type) }; refreshDevices() }
    fun connectAddressChanged(address: String) = _state.update { it.copy(connectAddress = address) }
    fun deviceSelected(target: PrinterTarget) =
        _state.update { it.copy(connectType = target.type, connectAddress = target.address) }
    fun disconnect() = printer.disconnect()
    fun dismissMessage() = _state.update { it.copy(printMessageAr = null) }

    fun refreshDevices() {
        val devices = when (_state.value.connectType) {
            PrinterType.BLUETOOTH -> printer.discoverBluetooth()
            PrinterType.USB -> printer.discoverUsb()
            PrinterType.SERIAL -> printer.discoverSerialPorts()
            PrinterType.NETWORK -> emptyList()
        }
        _state.update { it.copy(discoveredDevices = devices) }
    }

    fun connect() {
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
                is PrintResult.Success -> _state.update { it.copy(showConnectDialog = false) }
                is PrintResult.Failure -> _state.update { it.copy(printMessageAr = result.message) }
            }
        }
    }

    /** Print the captured receipt PNG. Fails fast if not connected. */
    fun print(png: ByteArray) {
        if (_state.value.printerState !is PrinterState.Connected) return
        _state.update { it.copy(isPrinting = true, pendingPrint = false, printMessageAr = null) }
        viewModelScope.launch {
            val result = printer.printImage(png, PaperWidth.MM80)
            _state.update {
                it.copy(
                    isPrinting = false,
                    printMessageAr = when (result) {
                        is PrintResult.Success -> SUCCESS_MESSAGE
                        is PrintResult.Failure -> result.message
                    },
                )
            }
        }
    }

    private companion object {
        const val SUCCESS_MESSAGE = "تمت الطباعة بنجاح"
    }
}
