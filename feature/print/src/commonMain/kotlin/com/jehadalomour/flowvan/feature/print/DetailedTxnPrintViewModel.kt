package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.DetailedTxnReport
import com.jehadalomour.flowvan.core.data.repository.TransactionReportRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class DetailedTxnPrintState(
    val isLoading: Boolean = true,
    val customerNameAr: String = "",
    val customerCode: String = "",
    val customerPhone: String = "",
    val salesmanNameAr: String = "",
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val printedAt: Long = 0L,
    val report: DetailedTxnReport = DetailedTxnReport(),
    val errorAr: String? = null,
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    val companyLogo: String = "",
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
)

@OptIn(ExperimentalTime::class)
class DetailedTxnPrintViewModel(
    private val customerId: String,
    private val fromMillis: Long,
    private val toMillis: Long,
    private val customers: CustomerRepository,
    private val users: UserRepository,
    private val reports: TransactionReportRepository,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DetailedTxnPrintState(
            fromMillis = fromMillis,
            toMillis = toMillis,
            printedAt = Clock.System.now().toEpochMilliseconds(),
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<DetailedTxnPrintState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val customer = customers.findById(customerId)
            val salesman = session.currentUserId?.let { users.findById(it) }
            _state.update {
                it.copy(
                    customerNameAr = customer?.nameAr.orEmpty(),
                    customerCode = customer?.code.orEmpty(),
                    customerPhone = customer?.phone.orEmpty(),
                    salesmanNameAr = salesman?.nameAr.orEmpty(),
                )
            }
            val report = reports.loadDetailed(
                customerNumber = customer?.code.orEmpty(),
                customerId = customerId,
                from = fromMillis.isoDate(),
                to = toMillis.isoDate(),
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    report = report,
                    errorAr = if (report.isLive) null else ERR_OFFLINE,
                )
            }
        }

        printer.state.onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val info = companyInfo.getForPrint()
            _state.update {
                it.copy(
                    companyNameAr = info.nameAr,
                    companyNameEn = info.nameEn,
                    companyTaxNumber = info.taxNumber,
                    companyLogo = info.logo,
                )
            }
        }
    }

    fun onEvent(event: TxnReportPrintEvent) {
        when (event) {
            TxnReportPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }
            TxnReportPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }
            is TxnReportPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }
            is TxnReportPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }
            is TxnReportPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }
            TxnReportPrintEvent.RefreshDevices -> refreshDevices()
            TxnReportPrintEvent.Connect -> connect()
            TxnReportPrintEvent.Disconnect -> printer.disconnect()
            is TxnReportPrintEvent.Print -> print(event.png)
            TxnReportPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
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
        viewModelScope.launch {
            val result = printer.connect(
                PrinterTarget(
                    type = s.connectType,
                    address = s.connectAddress,
                    name = s.connectAddress,
                    baudRate = printer.lastTarget?.baudRate ?: 115200,
                ),
            )
            when (result) {
                is PrintResult.Success -> _state.update { it.copy(showConnectDialog = false) }
                is PrintResult.Failure -> _state.update { it.copy(printMessageAr = result.message) }
            }
        }
    }

    private fun print(png: ByteArray) {
        if (_state.value.printerState !is PrinterState.Connected) return
        _state.update { it.copy(isPrinting = true, pendingPrint = false, printMessageAr = null) }
        viewModelScope.launch {
            val result = printer.printImage(png, PaperWidth.MM80)
            _state.update {
                it.copy(
                    isPrinting = false,
                    printMessageAr = when (result) {
                        is PrintResult.Success -> SUCCESS
                        is PrintResult.Failure -> result.message
                    },
                )
            }
        }
    }

    private companion object {
        const val SUCCESS = "تمت الطباعة بنجاح"
        const val ERR_OFFLINE = "تعذّر جلب الحركات من الخادم. لا يمكن طباعة التقرير دون اتصال."
    }
}

@OptIn(ExperimentalTime::class)
private fun Long.isoDate(): String {
    val d = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${d.year}-${d.monthNumber.toString().padStart(2, '0')}-" +
        d.dayOfMonth.toString().padStart(2, '0')
}
