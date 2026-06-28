package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
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

enum class SalesTypeFilter { ALL, SALE, RETURN, REQUEST }

data class AllSalesReportState(
    val from: Long = 0L,
    val to: Long = 0L,
    val typeFilter: SalesTypeFilter = SalesTypeFilter.SALE,
    val invoices: List<InvoiceEntity> = emptyList(),
    val salesTotal: Double = 0.0,
    val returnsTotal: Double = 0.0,
    val count: Int = 0,
)

class AllSalesReportViewModel(private val invoiceDao: InvoiceDao) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)
    private val _typeFilter = MutableStateFlow(SalesTypeFilter.SALE)

    private val _state = MutableStateFlow(AllSalesReportState())
    val state: StateFlow<AllSalesReportState> = _state.asStateFlow()

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
        val to = nowMs
        _from.value = from
        _to.value = to
        _state.update { it.copy(from = from, to = to) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_from, _to, _typeFilter) { f, t, type -> Triple(f, t, type) }
            .flatMapLatest { (f, t, type) ->
                if (type == SalesTypeFilter.ALL) invoiceDao.observeAllByRange(f, t)
                else invoiceDao.observeAllByTypeAndRange(type.name, f, t)
            }
            .onEach { list ->
                val sales = list.filter { it.type == "SALE" }.sumOf { it.total }
                val returns = list.filter { it.type == "RETURN" }.sumOf { it.total }
                _state.update {
                    it.copy(
                        from = _from.value, to = _to.value, typeFilter = _typeFilter.value,
                        invoices = list, salesTotal = sales, returnsTotal = returns, count = list.size,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
    fun setTypeFilter(f: SalesTypeFilter) { _typeFilter.value = f }
}
