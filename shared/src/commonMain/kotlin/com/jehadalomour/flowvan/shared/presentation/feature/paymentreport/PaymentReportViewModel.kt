package com.jehadalomour.flowvan.shared.presentation.feature.paymentreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.PaymentDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PaymentReportViewModel(
    private val customerId: String,
    private val paymentDao: PaymentDao,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PaymentReportState(
            customerId = customerId,
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        )
    )
    val state: StateFlow<PaymentReportState> = _state.asStateFlow()

    init {
        observePayments()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePayments() {
        _state
            .map { Triple(it.methodFilter, it.fromMillis, it.toMillis) }
            .distinctUntilChanged()
            .flatMapLatest { (method, from, to) ->
                when (method) {
                    PaymentMethodFilter.ALL -> paymentDao.observeByCustomerRange(customerId, from, to)
                    PaymentMethodFilter.CASH -> paymentDao.observeByCustomerMethodRange(customerId, "CASH", from, to)
                    PaymentMethodFilter.CHEQUE -> paymentDao.observeByCustomerMethodRange(customerId, "CHEQUE", from, to)
                    PaymentMethodFilter.TRANSFER -> paymentDao.observeByCustomerMethodRange(customerId, "TRANSFER", from, to)
                }
            }
            .onEach { payments -> _state.update { it.copy(payments = payments, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PaymentReportEvent) {
        when (event) {
            is PaymentReportEvent.MethodFilterChanged ->
                _state.update { it.copy(methodFilter = event.filter, isLoading = true) }
            is PaymentReportEvent.DateRangeChanged ->
                _state.update { it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis, isLoading = true) }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun startOfMonthMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
    return LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
private fun endOfTodayMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
    return today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds() - 1
}
