package com.jehadalomour.flowvan.shared.presentation.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.CustomerDao
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.local.entity.CustomerEntity
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

data class VisitedCustomer(val customer: CustomerEntity, val visited: Boolean, val invoiceCount: Int, val salesTotal: Double)

data class VisitReportState(
    val from: Long = 0L,
    val to: Long = 0L,
    val customers: List<VisitedCustomer> = emptyList(),
    val visitedCount: Int = 0,
    val plannedCount: Int = 0,
    val visitRate: Float = 0f,
    val totalSales: Double = 0.0,
)

class VisitReportViewModel(
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)

    private val _state = MutableStateFlow(VisitReportState())
    val state: StateFlow<VisitReportState> = _state.asStateFlow()

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
                    customerDao.observeRouteCustomers(),
                    invoiceDao.observeAllByRange(f, t),
                ) { customers, invoices ->
                    val invoicesByCustomer = invoices.groupBy { it.customerId }
                    customers.map { c ->
                        val cInvoices = invoicesByCustomer[c.id] ?: emptyList()
                        val sales = cInvoices.filter { it.type == "SALE" }.sumOf { it.total }
                        VisitedCustomer(c, cInvoices.isNotEmpty(), cInvoices.size, sales)
                    }
                }
            }
            .onEach { list ->
                val visited = list.count { it.visited }
                val total = list.size
                _state.update {
                    it.copy(
                        from = _from.value, to = _to.value,
                        customers = list,
                        visitedCount = visited,
                        plannedCount = total,
                        visitRate = if (total > 0) visited.toFloat() / total else 0f,
                        totalSales = list.sumOf { it.salesTotal },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
}
