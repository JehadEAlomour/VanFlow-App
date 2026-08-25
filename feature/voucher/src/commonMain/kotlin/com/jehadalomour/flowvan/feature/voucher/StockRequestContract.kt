package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.network.dto.StockRequestDto

/**
 * Asking the warehouse to load stock onto this van.
 *
 * Deliberately the SAME shape as the voucher flow: a picker you add items from,
 * a cart you review, one send. A rep does this between selling, and a second
 * item-entry idiom to learn is a second thing to get wrong under time pressure.
 *
 * What is NOT here is everything about money — no price, no discount, no tax, no
 * customer, no payment. A stock request totals a QUANTITY. Reusing [CartLine]
 * means those fields exist and simply stay at their defaults; the alternative
 * was a parallel line type that could not use any of the shared cart UI.
 *
 * The unit still matters as much as it does on a sale: a variant unit owns its
 * own stock pool, so the pool a line draws on is (productId, unitId) — the same
 * identity the cart is keyed on.
 */
/** Within the "new request" flow: pick items, then review the cart. */
enum class StockRequestView { PICKER, CART }

/**
 * The screen's two halves, deliberately split:
 *  - NEW  — build and send a request (picker → cart).
 *  - MINE — track requests already sent (status, and the button that receives them).
 * They answer two different questions ("what do I need?" vs "am I getting it?"),
 * so mixing them in one scroll made each harder to act on.
 */
enum class StockRequestTab { NEW, MINE }

data class StockRequestState(
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    /** Units per product id, as synced. Empty list = the item has only its base unit. */
    val productUnits: Map<String, List<ProductUnit>> = emptyMap(),

    /** Main-depot stock per pool, keyed "itemNumber|stockUnitCode". A van load cannot exceed it. */
    val mainStock: Map<String, Double> = emptyMap(),
    val mainStoreName: String? = null,

    val cart: List<CartLine> = emptyList(),
    /** Which half of the screen is showing. */
    val tab: StockRequestTab = StockRequestTab.NEW,
    /** Within the NEW tab: the item picker or the cart review. */
    val view: StockRequestView = StockRequestView.PICKER,
    val searchQuery: String = "",
    val note: String = "",

    val isSubmitting: Boolean = false,

    /**
     * This rep's recent requests. Kept on the same screen rather than a separate
     * one because the rep's real question after sending is "am I getting it?",
     * and the answer — including the button that receives the goods — lands here.
     */
    val mine: List<StockRequestDto> = emptyList(),
    val isLoadingMine: Boolean = false,
    /** Ids with a receive/cancel call in flight, so each row spins alone. */
    val busyIds: Set<String> = emptySet(),

    val errorAr: String? = null,
    val noticeAr: String? = null,
) {
    val canSubmit: Boolean get() = cart.isNotEmpty() && !isSubmitting

    /** Total pieces requested — the only "total" a stock request has. */
    val totalBaseQty: Double get() = cart.sumOf { it.stockQty }

    /** Distinct items, for the cart badge. A 2-unit item still counts as 2 lines. */
    val lineCount: Int get() = cart.size

    /** Requests still in play (awaiting a decision or awaiting receipt) — the MINE tab badge. */
    val activeCount: Int get() = mine.count { it.status == "pending" || it.status == "approved" }

    /** Units for one product, smallest first — the order a rep reads them in. */
    fun unitsFor(productId: String): List<ProductUnit> =
        (productUnits[productId] ?: emptyList()).sortedBy { it.conversionQty }

    /** Base pieces the main depot holds for this product+unit's pool. */
    fun availableBase(sku: String, unit: ProductUnit): Double =
        mainStock["$sku|${if (unit.isStockUnit) unit.code else ""}"] ?: 0.0

    /** Cart quantity per product, for the picker's badge. Summed across units. */
    val cartQtyByProduct: Map<String, Double>
        get() = cart.groupBy { it.productId }.mapValues { (_, ls) -> ls.sumOf { it.qty } }
}

sealed interface StockRequestEvent {
    /** Switch between the "new request" and "my requests" halves. */
    data class SelectTab(val tab: StockRequestTab) : StockRequestEvent

    /** Swap between the item picker and the cart. */
    data object ToggleView : StockRequestEvent
    data class SearchChanged(val v: String) : StockRequestEvent

    /**
     * Add or edit one line. [unit] carries the conversion factor and the pool,
     * so the line can be rebuilt without re-reading the catalogue.
     */
    data class ConfirmItem(val product: Product, val qty: Double, val unit: ProductUnit) :
        StockRequestEvent
    data class RemoveLine(val productId: String, val unitId: String) : StockRequestEvent
    data object ClearCart : StockRequestEvent

    data class NoteChanged(val v: String) : StockRequestEvent

    /** Send the cart to the office. */
    data object Submit : StockRequestEvent

    /** Withdraw a request the office has not answered yet. */
    data class Cancel(val id: String) : StockRequestEvent

    /**
     * Confirm the goods are on the van. This is what moves the stock — the
     * server raises the transfer here, not when the office approved.
     */
    data class Receive(val id: String) : StockRequestEvent

    data object Refresh : StockRequestEvent
    data object DismissError : StockRequestEvent
}
