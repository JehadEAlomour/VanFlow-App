package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.ShiftDao
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.model.Shift
import com.jehadalomour.flowvan.core.model.ShiftStatus
import com.jehadalomour.flowvan.core.domain.usecase.EndShiftUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.core.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow(EndOfDayState())
    val state: StateFlow<EndOfDayState> = _state.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: EndOfDayEvent) {
        when (event) {
            EndOfDayEvent.OpenConfirmDialog -> _state.update { it.copy(showConfirmDialog = true) }
            EndOfDayEvent.DismissConfirmDialog -> _state.update { it.copy(showConfirmDialog = false) }
            EndOfDayEvent.ConfirmEndShift -> confirmEndShift()
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

            _state.update { s ->
                s.copy(
                    kpi = kpi,
                    cashCollectedToday = cash,
                    chequesCollectedToday = cheques,
                    transfersCollectedToday = transfers,
                    unsyncedInvoices = unsyncedInv,
                    unsyncedPayments = unsyncedPay,
                    activeShift = activeShift,
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
}
