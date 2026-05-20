package com.jehadalomour.flowvan.shared.presentation.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
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
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ItemSalesRow(
    val productId: String,
    val sku: String,
    val nameAr: String,
    val totalQty: Double,
    val totalAmount: Double,
    val invoiceCount: Int,
)

data class ItemsSalesReportState(
    val from: Long = 0L,
    val to: Long = 0L,
    val items: List<ItemSalesRow> = emptyList(),
    val grandTotalQty: Double = 0.0,
    val grandTotalAmount: Double = 0.0,
)

class ItemsSalesReportViewModel(private val invoiceDao: InvoiceDao) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)

    private val _state = MutableStateFlow(ItemsSalesReportState())
    val state: StateFlow<ItemsSalesReportState> = _state.asStateFlow()

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
            .flatMapLatest { (f, t) -> invoiceDao.observeAllByTypeAndRange("SALE", f, t) }
            .onEach { invoices ->
                // Parse lines from each SALE invoice and aggregate by product
                data class Acc(var qty: Double = 0.0, var amount: Double = 0.0, var count: Int = 0, val sku: String, val nameAr: String)
                val map = mutableMapOf<String, Acc>()

                invoices.forEach { inv ->
                    try {
                        val lines = json.decodeFromString<List<InvoiceLine>>(inv.linesJson)
                        lines.forEach { line ->
                            val acc = map.getOrPut(line.productId) { Acc(sku = line.sku, nameAr = line.nameAr) }
                            acc.qty += line.qty
                            acc.amount += line.lineTotal
                            acc.count++
                        }
                    } catch (_: Exception) { }
                }

                val rows = map.entries
                    .map { (pid, acc) -> ItemSalesRow(pid, acc.sku, acc.nameAr, acc.qty, acc.amount, acc.count) }
                    .sortedByDescending { it.totalAmount }

                _state.update {
                    it.copy(
                        from = _from.value, to = _to.value,
                        items = rows,
                        grandTotalQty = rows.sumOf { it.totalQty },
                        grandTotalAmount = rows.sumOf { it.totalAmount },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
}
