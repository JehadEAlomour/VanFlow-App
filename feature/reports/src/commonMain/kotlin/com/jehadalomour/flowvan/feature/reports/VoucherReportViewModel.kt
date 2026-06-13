package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private data class Filters(
    val type: VoucherTypeFilter,
    val kind: VoucherKindFilter,
    val from: Long,
    val to: Long,
)

class VoucherReportViewModel(
    private val customerId: String,
    private val invoiceDao: InvoiceDao,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoucherReportState(
            customerId = customerId,
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        )
    )
    val state: StateFlow<VoucherReportState> = _state.asStateFlow()

    init {
        observeInvoices()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeInvoices() {
        _state
            .map { Filters(it.typeFilter, it.kindFilter, it.fromMillis, it.toMillis) }
            .distinctUntilChanged()
            .flatMapLatest { f ->
                val baseFlow = when (f.type) {
                    VoucherTypeFilter.ALL -> invoiceDao.observeByCustomerRange(customerId, f.from, f.to)
                    VoucherTypeFilter.SALE -> invoiceDao.observeByCustomerTypeRange(customerId, "SALE", f.from, f.to)
                    VoucherTypeFilter.RETURN -> invoiceDao.observeByCustomerTypeRange(customerId, "RETURN", f.from, f.to)
                    VoucherTypeFilter.REQUEST -> invoiceDao.observeByCustomerTypeRange(customerId, "REQUEST", f.from, f.to)
                }
                baseFlow.map { invoices -> invoices.applyKind(f.kind) }
            }
            .onEach { invoices -> _state.update { it.copy(invoices = invoices, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: VoucherReportEvent) {
        when (event) {
            is VoucherReportEvent.TypeFilterChanged ->
                _state.update { it.copy(typeFilter = event.filter, isLoading = true) }
            is VoucherReportEvent.KindFilterChanged ->
                _state.update { it.copy(kindFilter = event.filter, isLoading = true) }
            is VoucherReportEvent.DateRangeChanged ->
                _state.update { it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis, isLoading = true) }
        }
    }

    private fun List<InvoiceEntity>.applyKind(kind: VoucherKindFilter): List<InvoiceEntity> = when (kind) {
        VoucherKindFilter.ALL -> this
        VoucherKindFilter.CASH -> filter { it.paymentMethod == "CASH" }
        VoucherKindFilter.CHEQUE -> filter { it.paymentMethod == "CHEQUE" }
        VoucherKindFilter.TRANSFER -> filter { it.paymentMethod == "TRANSFER" }
        VoucherKindFilter.CREDIT -> filter { it.paymentMethod == "CREDIT" || it.paymentMethod == null }
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
