package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
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
class CashFlowPrintViewModel(
    private val fromMillis: Long,
    private val toMillis: Long,
    private val invoices: InvoiceDao,
    private val payments: PaymentDao,
    private val users: UserRepository,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CashFlowPrintState(
            fromMillis = fromMillis,
            toMillis = toMillis,
            printedAt = Clock.System.now().toEpochMilliseconds(),
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectLanguage = printer.language,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<CashFlowPrintState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val salesman = session.currentUserId?.let { users.findById(it) }
            // A snapshot, not a subscription: the paper is a document, and a total
            // that moves while it is being handed over is not one.
            val inv = invoices.observeAllByRange(fromMillis, toMillis).first()
            val pay = payments.observeAllByRange(fromMillis, toMillis).first()

            // EVERY rule below mirrors CashFlowReportViewModel exactly — same splits,
            // same null handling. The rep reads the screen and hands over the paper;
            // if the two ever disagree the document is worse than useless.
            val sales = inv.filter { it.type == SALE }
            val returns = inv.filter { it.type == RETURN }

            // A null method counts as CREDIT (see CreateSaleVoucherUseCase).
            val salesCash = sales.filter { it.paymentMethod == CASH }.sumOf { it.total }
            val salesCredit = sales
                .filter { it.paymentMethod == CREDIT || it.paymentMethod == null }
                .sumOf { it.total }

            // A CREDIT return is a credit note; anything else — CASH, or an older
            // return saved with no method at all — took cash back out of the bag.
            val returnsCredit = returns.filter { it.paymentMethod == CREDIT }.sumOf { it.total }
            val returnsCash = returns.filter { it.paymentMethod != CREDIT }.sumOf { it.total }

            // Other collection methods still roll into the overall total.
            val collectionsCash = pay.filter { it.method == CASH }.sumOf { it.amount }
            val collectionsCheque = pay.filter { it.method == CHEQUE }.sumOf { it.amount }

            _state.update {
                it.copy(
                    isLoading = false,
                    salesmanNameAr = salesman?.nameAr.orEmpty(),
                    salesCashTotal = salesCash,
                    salesCreditTotal = salesCredit,
                    salesTotal = sales.sumOf { s -> s.total },
                    salesCount = sales.size,
                    returnsCashTotal = returnsCash,
                    returnsCreditTotal = returnsCredit,
                    returnsTotal = returns.sumOf { r -> r.total },
                    returnsCount = returns.size,
                    collectionsCashTotal = collectionsCash,
                    collectionsChequeTotal = collectionsCheque,
                    collectionsTotal = pay.sumOf { p -> p.amount },
                    collectionsCount = pay.size,
                    totalCash = salesCash + collectionsCash - returnsCash,
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

    fun onEvent(event: CashFlowPrintEvent) {
        when (event) {
            CashFlowPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }
            CashFlowPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }
            is CashFlowPrintEvent.PrinterLanguageSelected -> {
                // Device-wide and persisted: writing it here routes every print
                // screen to the right SDK from now on, not just this one.
                printer.language = event.language
                _state.update { it.copy(connectLanguage = event.language) }
            }

            is CashFlowPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }
            is CashFlowPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }
            is CashFlowPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }
            CashFlowPrintEvent.RefreshDevices -> refreshDevices()
            CashFlowPrintEvent.Connect -> connect()
            CashFlowPrintEvent.Disconnect -> printer.disconnect()
            is CashFlowPrintEvent.Print -> print(event.png)
            CashFlowPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
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
        const val SALE = "SALE"
        const val RETURN = "RETURN"
        const val CASH = "CASH"
        const val CREDIT = "CREDIT"
        const val CHEQUE = "CHEQUE"
    }
}
