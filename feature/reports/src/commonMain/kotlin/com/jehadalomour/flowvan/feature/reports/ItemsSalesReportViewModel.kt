package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.model.InvoiceLine
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
    /**
     * `item_units.id` of the unit sold, "" = the item's base pool.
     *
     * Part of the row IDENTITY, not decoration: one item sells in several units,
     * and an offer commonly gives a piece free against a carton sold. Keyed on the
     * item alone those became ONE row that was part-free at a blended rate — which
     * is neither of the two things that actually happened.
     */
    val unitId: String,
    /** Display name of that unit (حبة، كرتونة، أحمر…). */
    val unitName: String,
    /** Every unit that left the van, gifts included — this is the stock reality. */
    val totalQty: Double,
    /**
     * How many of [totalQty] were given away by an offer.
     *
     * Reported separately because 15 units at a given revenue reads as 15 sold;
     * if 3 were giveaways the item's real rate per unit is quite different, and
     * a manager judging performance off this screen would be misled.
     */
    val freeQty: Double,
    val totalAmount: Double,
    val invoiceCount: Int,
) {
    val paidQty: Double get() = (totalQty - freeQty).coerceAtLeast(0.0)
    val hasFree: Boolean get() = freeQty > 0.0005
}

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
                data class Acc(
                    var qty: Double = 0.0,
                    var freeQty: Double = 0.0,
                    var amount: Double = 0.0,
                    var count: Int = 0,
                    val sku: String,
                    val nameAr: String,
                    val productId: String,
                    val unitId: String,
                    val unitName: String,
                )
                val map = mutableMapOf<String, Acc>()

                invoices.forEach { inv ->
                    try {
                        val lines = json.decodeFromString<List<InvoiceLine>>(inv.linesJson)
                        lines.forEach { line ->
                            // Composite key. The separator cannot appear in a uuid, so
                            // "item A / unit B" can never collide with "item AB / none".
                            val key = "${line.productId}|${line.unitId}"
                            val acc = map.getOrPut(key) {
                                Acc(
                                    sku = line.sku,
                                    nameAr = line.nameAr,
                                    productId = line.productId,
                                    unitId = line.unitId,
                                    unitName = line.unit,
                                )
                            }
                            acc.qty += line.qty
                            // An offer's free item is a normal PRICED line at 100%
                            // discount — "fully discounted" is the only marker there is.
                            // Its lineTotal is already 0, so the revenue is right; what
                            // was missing is that the quantity was not paid for.
                            if (line.discountPct >= 0.999) acc.freeQty += line.qty
                            acc.amount += line.lineTotal
                            acc.count++
                        }
                    } catch (_: Exception) { }
                }

                val rows = map.entries
                    .map { (_, acc) ->
                        ItemSalesRow(
                            productId = acc.productId,
                            sku = acc.sku,
                            nameAr = acc.nameAr,
                            unitId = acc.unitId,
                            unitName = acc.unitName,
                            totalQty = acc.qty,
                            freeQty = acc.freeQty,
                            totalAmount = acc.amount,
                            invoiceCount = acc.count,
                        )
                    }
                    // Item first, then its units together — a free unit sits directly
                    // under the paid unit it came with, instead of being flung to the
                    // bottom of the list by its zero revenue.
                    .sortedWith(
                        compareByDescending<ItemSalesRow> { row ->
                            map.values.filter { it.productId == row.productId }.sumOf { it.amount }
                        }.thenBy { it.nameAr }.thenByDescending { it.totalAmount },
                    )

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
