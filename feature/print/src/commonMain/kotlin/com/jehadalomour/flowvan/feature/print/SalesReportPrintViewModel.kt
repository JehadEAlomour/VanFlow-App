package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SalesReportPrintViewModel(
    private val fromMillis: Long,
    private val toMillis: Long,
    private val invoices: InvoiceDao,
    private val customers: CustomerDao,
    private val users: UserRepository,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SalesReportPrintState(
            fromMillis = fromMillis,
            toMillis = toMillis,
            printedAt = Clock.System.now().toEpochMilliseconds(),
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<SalesReportPrintState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val salesman = session.currentUserId?.let { users.findById(it) }
            // A snapshot, not a subscription: the paper is a document, and a total that
            // changes while it is being handed over is not one.
            val all = invoices.observeAllByRange(fromMillis, toMillis).first()
            val names = customers.observeAll().first().associate { it.id to it.nameAr }

            // The same exclusions and the same cash/credit rule as the screen — a
            // printout that disagrees with the screen it was opened from is the one
            // bug this whole document cannot survive.
            val live = all.filter { it.status != CANCELLED }
            val sales = live.filter { it.type == "SALE" }
            val returns = live.filter { it.type == "RETURN" }
            val requests = live.filter { it.type != "SALE" && it.type != "RETURN" }
            val salesTotal = sales.sumOf { it.total }
            val returnsTotal = returns.sumOf { it.total }

            _state.update {
                it.copy(
                    isLoading = false,
                    salesmanNameAr = salesman?.nameAr.orEmpty(),
                    rows = live
                        .sortedBy { inv -> inv.createdAt }
                        .map { inv ->
                            SalesReportPrintRow(
                                number = inv.number,
                                customerNameAr = names[inv.customerId].orEmpty(),
                                dateMillis = inv.createdAt,
                                type = inv.type,
                                total = inv.total,
                                isCredit = inv.type == "SALE" && inv.paymentMethod == CREDIT,
                            )
                        },
                    salesTotal = salesTotal,
                    returnsTotal = returnsTotal,
                    requestsTotal = requests.sumOf { r -> r.total },
                    cashTotal = sales.filter { s -> s.paymentMethod != CREDIT }.sumOf { s -> s.total },
                    creditTotal = sales.filter { s -> s.paymentMethod == CREDIT }.sumOf { s -> s.total },
                    netTotal = salesTotal - returnsTotal,
                    count = live.size,
                )
            }
        }

        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
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

    fun onEvent(event: SalesReportPrintEvent) {
        when (event) {
            SalesReportPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }
            SalesReportPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }
            is SalesReportPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }
            is SalesReportPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }
            is SalesReportPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }
            SalesReportPrintEvent.RefreshDevices -> refreshDevices()
            SalesReportPrintEvent.Connect -> connect()
            SalesReportPrintEvent.Disconnect -> printer.disconnect()
            is SalesReportPrintEvent.Print -> print(event.png)
            SalesReportPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
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
                is PrintResult.Success -> _state.update { it.copy(showConnectDialog = false) }
                is PrintResult.Failure -> _state.update { it.copy(printMessageAr = result.message) }
            }
        }
    }

    /** As an image: printer firmware cannot shape Arabic. */
    private fun print(png: ByteArray) {
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
        const val CANCELLED = "CANCELLED"
        const val CREDIT = "CREDIT"
    }
}
