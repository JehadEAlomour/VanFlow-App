package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed class CashEntry(val timestampMs: Long) {
    class Sale(val invoice: InvoiceEntity) : CashEntry(invoice.createdAt)
    class Return(val invoice: InvoiceEntity) : CashEntry(invoice.createdAt)
    class Collection(val payment: PaymentEntity) : CashEntry(payment.createdAt)
}

data class CashFlowReportState(
    val from: Long = 0L,
    val to: Long = 0L,
    val entries: List<CashEntry> = emptyList(),
    val salesTotal: Double = 0.0,
    val returnsTotal: Double = 0.0,
    val collectionsTotal: Double = 0.0,
    val netCash: Double = 0.0,
)

class CashFlowReportViewModel(
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)

    private val _state = MutableStateFlow(CashFlowReportState())
    val state: StateFlow<CashFlowReportState> = _state.asStateFlow()

    init {
        initDefaults()
        observe()
    }

    @OptIn(ExperimentalTime::class)
    private fun initDefaults() {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val from = today.atStartOfDayIn(tz).toEpochMilliseconds()
        _from.value = from; _to.value = nowMs
        _state.update { it.copy(from = from, to = nowMs) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_from, _to) { f, t -> f to t }
            .flatMapLatest { (f, t) ->
                combine(
                    invoiceDao.observeAllByRange(f, t),
                    paymentDao.observeAllByRange(f, t),
                ) { invoices, payments -> invoices to payments }
            }
            .onEach { (invoices, payments) ->
                val entries = buildList {
                    invoices.forEach { inv ->
                        if (inv.type == "SALE") add(CashEntry.Sale(inv))
                        else if (inv.type == "RETURN") add(CashEntry.Return(inv))
                    }
                    payments.forEach { pay -> add(CashEntry.Collection(pay)) }
                }.sortedByDescending { it.timestampMs }

                val sales = invoices.filter { it.type == "SALE" }.sumOf { it.total }
                val returns = invoices.filter { it.type == "RETURN" }.sumOf { it.total }
                val collections = payments.sumOf { it.amount }

                _state.update {
                    it.copy(
                        from = _from.value, to = _to.value,
                        entries = entries,
                        salesTotal = sales,
                        returnsTotal = returns,
                        collectionsTotal = collections,
                        netCash = collections - returns,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
}
