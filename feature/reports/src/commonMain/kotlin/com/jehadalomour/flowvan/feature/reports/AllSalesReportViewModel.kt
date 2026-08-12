package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.CustomerDao
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
    val typeFilter: SalesTypeFilter = SalesTypeFilter.ALL,
    /** The documents the chip is showing. */
    val rows: List<InvoiceEntity> = emptyList(),
    // ── Totals — always the WHOLE period, never the filtered view ──────────────
    val salesTotal: Double = 0.0,
    val returnsTotal: Double = 0.0,
    val requestsTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val creditTotal: Double = 0.0,
    /** Sales less returns: what the round actually earned. */
    val netTotal: Double = 0.0,
    /** Documents in the period, unfiltered. */
    val count: Int = 0,
    /** customerId → display name (Arabic), for showing whose voucher each row is. */
    val customerNames: Map<String, String> = emptyMap(),
)

class AllSalesReportViewModel(
    private val invoiceDao: InvoiceDao,
    private val customerDao: CustomerDao,
) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)
    private val _typeFilter = MutableStateFlow(SalesTypeFilter.ALL)

    private val _state = MutableStateFlow(AllSalesReportState())
    val state: StateFlow<AllSalesReportState> = _state.asStateFlow()

    init {
        initDefaults()
        observe()
        observeCustomers()
    }

    private fun observeCustomers() {
        customerDao.observeAll()
            .onEach { customers ->
                _state.update { s -> s.copy(customerNames = customers.associate { it.id to it.nameAr }) }
            }
            .launchIn(viewModelScope)
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

    /**
     * One query for the whole period, filtered in memory.
     *
     * The totals used to be summed from the type-filtered list, and the screen opens on
     * a filter — so الصافي read "sales minus zero returns" on the default view, which is
     * the one figure a rep checks at the end of a round. Totals now describe the period;
     * a chip is a way of looking at it, not a claim about what was sold.
     *
     * Cancelled and rejected vouchers are excluded, as they are everywhere else that
     * counts money — a rejected return that still adds to a total is a report nobody can
     * reconcile against the office.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_from, _to) { f, t -> f to t }
            .flatMapLatest { (f, t) -> invoiceDao.observeAllByRange(f, t) }
            .combine(_typeFilter) { all, filter -> all to filter }
            .onEach { (all, filter) ->
                val live = all.filter { it.status != CANCELLED }
                val sales = live.filter { it.type == "SALE" }
                val returns = live.filter { it.type == "RETURN" }
                val requests = live.filter { it.type != "SALE" && it.type != "RETURN" }
                val salesTotal = sales.sumOf { it.total }
                val returnsTotal = returns.sumOf { it.total }
                _state.update {
                    it.copy(
                        from = _from.value,
                        to = _to.value,
                        typeFilter = filter,
                        rows = when (filter) {
                            SalesTypeFilter.ALL -> live
                            SalesTypeFilter.SALE -> sales
                            SalesTypeFilter.RETURN -> returns
                            SalesTypeFilter.REQUEST -> requests
                        },
                        salesTotal = salesTotal,
                        returnsTotal = returnsTotal,
                        requestsTotal = requests.sumOf { r -> r.total },
                        // Cash and credit describe SALES only. A return's payment type
                        // says how the refund went out, not how a sale came in, and
                        // mixing the two makes "نقدي" answer no question.
                        cashTotal = sales.filter { s -> s.paymentMethod != CREDIT }.sumOf { s -> s.total },
                        creditTotal = sales.filter { s -> s.paymentMethod == CREDIT }.sumOf { s -> s.total },
                        netTotal = salesTotal - returnsTotal,
                        count = live.size,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
    fun setTypeFilter(f: SalesTypeFilter) { _typeFilter.value = f }

    private companion object {
        const val CANCELLED = "CANCELLED"
        const val CREDIT = "CREDIT"
    }
}
