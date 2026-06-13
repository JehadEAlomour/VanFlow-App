package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
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

class TransactionReportViewModel(
    private val customerId: String,
    private val invoiceDao: InvoiceDao,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TransactionReportState(
            customerId = customerId,
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        )
    )
    val state: StateFlow<TransactionReportState> = _state.asStateFlow()

    init {
        observeInvoices()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeInvoices() {
        _state
            .map { Triple(it.typeFilter, it.fromMillis, it.toMillis) }
            .distinctUntilChanged()
            .flatMapLatest { (type, from, to) ->
                when (type) {
                    TxnTypeFilter.ALL -> invoiceDao.observeByCustomerRange(customerId, from, to)
                    TxnTypeFilter.SALE -> invoiceDao.observeByCustomerTypeRange(customerId, "SALE", from, to)
                    TxnTypeFilter.RETURN -> invoiceDao.observeByCustomerTypeRange(customerId, "RETURN", from, to)
                    TxnTypeFilter.REQUEST -> invoiceDao.observeByCustomerTypeRange(customerId, "REQUEST", from, to)
                }
            }
            .onEach { invoices -> _state.update { it.copy(invoices = invoices, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: TransactionReportEvent) {
        when (event) {
            is TransactionReportEvent.TypeFilterChanged ->
                _state.update { it.copy(typeFilter = event.filter, isLoading = true) }
            is TransactionReportEvent.DateRangeChanged ->
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
