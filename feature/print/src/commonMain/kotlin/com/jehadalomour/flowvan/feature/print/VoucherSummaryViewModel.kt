package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.model.PaymentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class VoucherSummaryViewModel(
    private val invoiceDao: InvoiceDao,
    private val customerDao: CustomerDao,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val companyInfo: CompanyInfoRepository,
    private val appSettings: AppSettingsRepository,
    private val printer: ReceiptPrinter,
) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)

    private val _state = MutableStateFlow(
        VoucherSummaryState(
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<VoucherSummaryState> = _state.asStateFlow()

    init {
        initDefaults()
        observe()
        loadHeader()
        // Mirror the live printer connection state into our UI state.
        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalTime::class)
    private fun initDefaults() {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val from = today.atStartOfDayIn(tz).toEpochMilliseconds()
        _from.value = from
        _to.value = nowMs
        _state.update { it.copy(from = from, to = nowMs, reportAt = nowMs) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_from, _to) { f, t -> f to t }
            .flatMapLatest { (f, t) -> invoiceDao.observeAllByRange(f, t) }
            .combine(customerDao.observeAll()) { invoices, customers ->
                invoices to customers.associate { it.id to it.nameAr }
            }
            .onEach { (invoices, names) -> recompute(invoices, names) }
            .launchIn(viewModelScope)
    }

    /** Sale + return vouchers only (excludes REQUEST/quotes and cancelled). */
    private fun recompute(invoices: List<InvoiceEntity>, names: Map<String, String>) {
        val relevant = invoices.filter {
            (it.type == "SALE" || it.type == "RETURN") && it.status != "CANCELLED"
        }
        val rows = relevant.map { inv ->
            VoucherSummaryRow(
                id = inv.id,
                number = inv.number,
                customerName = names[inv.customerId].orEmpty(),
                type = inv.type,
                paymentType = PaymentType.fromPaymentMethod(inv.paymentMethod),
                total = inv.total,
                createdAt = inv.createdAt,
            )
        }

        val sales = rows.filter { it.type == "SALE" }
        val returns = rows.filter { it.type == "RETURN" }

        _state.update {
            it.copy(
                from = _from.value,
                to = _to.value,
                rows = rows,
                totalSales = sales.sumOf { r -> r.total },
                totalReturns = returns.sumOf { r -> r.total },
                cashSales = sales.filter { r -> r.paymentType == PaymentType.CASH }.sumOf { r -> r.total },
                cashReturns = returns.filter { r -> r.paymentType == PaymentType.CASH }.sumOf { r -> r.total },
                creditSales = sales.filter { r -> r.paymentType == PaymentType.CREDIT }.sumOf { r -> r.total },
                creditReturns = returns.filter { r -> r.paymentType == PaymentType.CREDIT }.sumOf { r -> r.total },
            )
        }
    }

    private fun loadHeader() {
        viewModelScope.launch {
            // Company header for the printed summary: server-first when online, else DB cache.
            val info = companyInfo.getForPrint()
            val settings = appSettings.get()
            val user = getCurrentUser()
            _state.update { s ->
                s.copy(
                    companyNameAr = info.nameAr,
                    companyNameEn = info.nameEn,
                    companyTaxNumber = info.taxNumber,
                    branch = settings.branch,
                    salesmanNameAr = user?.nameAr.orEmpty(),
                )
            }
        }
    }

    fun onEvent(event: VoucherSummaryEvent) {
        when (event) {
            is VoucherSummaryEvent.SetFrom -> {
                _from.value = event.millis
                _state.update { it.copy(from = event.millis) }
            }
            is VoucherSummaryEvent.SetTo -> {
                _to.value = event.millis
                _state.update { it.copy(to = event.millis) }
            }

            VoucherSummaryEvent.RequestConnectThenPrint -> {
                _state.update { it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null) }
                refreshDevices()
            }

            VoucherSummaryEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }

            is VoucherSummaryEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }

            is VoucherSummaryEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }

            is VoucherSummaryEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }

            VoucherSummaryEvent.RefreshDevices -> refreshDevices()
            VoucherSummaryEvent.Connect -> connect()
            VoucherSummaryEvent.Disconnect -> printer.disconnect()
            is VoucherSummaryEvent.Print -> print(event.receiptPng)
            VoucherSummaryEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
        }
    }

    private fun refreshDevices() {
        val type = _state.value.connectType
        val devices = when (type) {
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
    }
}
