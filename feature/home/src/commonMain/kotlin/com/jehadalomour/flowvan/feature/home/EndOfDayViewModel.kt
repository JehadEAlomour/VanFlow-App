package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.ShiftDao
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.model.Shift
import com.jehadalomour.flowvan.core.model.ShiftStatus
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import com.jehadalomour.flowvan.core.domain.usecase.EndShiftUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.core.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class EndOfDayViewModel(
    private val getKpi: GetDailyKpiUseCase,
    private val payments: PaymentRepository,
    private val invoices: InvoiceRepository,
    private val shiftDao: ShiftDao,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val endShift: EndShiftUseCase,
    private val logout: LogoutUseCase,
    private val appSettings: AppSettingsRepository,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EndOfDayState(
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
            printerLanguage = printer.language,
        ),
    )
    val state: StateFlow<EndOfDayState> = _state.asStateFlow()

    init {
        load()
        // Mirror the live printer connection state into our UI state.
        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EndOfDayEvent) {
        when (event) {
            EndOfDayEvent.OpenConfirmDialog -> _state.update { it.copy(showConfirmDialog = true) }
            EndOfDayEvent.DismissConfirmDialog -> _state.update { it.copy(showConfirmDialog = false) }
            EndOfDayEvent.ConfirmEndShift -> confirmEndShift()

            EndOfDayEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }

            EndOfDayEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }

            is EndOfDayEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }

            is EndOfDayEvent.PrinterLanguageSelected -> {
                // Device-wide setting: persist it on the printer so every print screen
                // routes to the right SDK (ESC/POS vs Zebra CPCL) from now on.
                printer.language = event.language
                _state.update { it.copy(printerLanguage = event.language) }
            }

            is EndOfDayEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }

            is EndOfDayEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }

            EndOfDayEvent.RefreshDevices -> refreshDevices()

            EndOfDayEvent.Connect -> connect()

            EndOfDayEvent.Disconnect -> printer.disconnect()

            is EndOfDayEvent.Print -> print(event.receiptPng)

            EndOfDayEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun load() {
        viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
            val startOfTodayMs = today.atStartOfDayIn(tz).toEpochMilliseconds()

            val kpi = getKpi()
            val cash = payments.totalByMethodSince("CASH", startOfTodayMs)
            val cheques = payments.totalByMethodSince("CHEQUE", startOfTodayMs)
            val transfers = payments.totalByMethodSince("TRANSFER", startOfTodayMs)
            val cashSales = invoices.cashOnlySalesTotalSince(startOfTodayMs)
            val cashReturns = invoices.cashReturnsTotalSince(startOfTodayMs)
            val unsyncedInv = invoices.countUnsyncedSince(startOfTodayMs)
            val unsyncedPay = payments.countUnsyncedSince(startOfTodayMs)

            val user = getCurrentUser()
            val activeShiftEntity = user?.let { shiftDao.findActive(it.id) }
            val activeShift = activeShiftEntity?.let {
                Shift(
                    id = it.id,
                    userId = it.userId,
                    startedAt = it.startedAt,
                    endedAt = it.endedAt,
                    status = ShiftStatus.valueOf(it.status),
                    startLat = it.startLat,
                    startLng = it.startLng,
                    endLat = it.endLat,
                    endLng = it.endLng,
                )
            }

            // Company header for the printed summary: server-first when online, else DB cache.
            val info = companyInfo.getForPrint()
            val settings = appSettings.get()

            _state.update { s ->
                s.copy(
                    kpi = kpi,
                    cashCollectedToday = cash,
                    chequesCollectedToday = cheques,
                    transfersCollectedToday = transfers,
                    cashSalesToday = cashSales,
                    cashReturnsToday = cashReturns,
                    unsyncedInvoices = unsyncedInv,
                    unsyncedPayments = unsyncedPay,
                    activeShift = activeShift,
                    salesmanNameAr = user?.nameAr.orEmpty(),
                    branch = settings.branch,
                    companyNameAr = info.nameAr,
                    companyNameEn = info.nameEn,
                    companyTaxNumber = info.taxNumber,
                    reportAt = nowMs,
                )
            }
        }
    }

    private fun confirmEndShift() {
        viewModelScope.launch {
            _state.update { it.copy(isEnding = true, showConfirmDialog = false) }
            _state.value.activeShift?.let { endShift(it.id) }
            logout()
            _state.update { it.copy(isEnding = false, done = true) }
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
