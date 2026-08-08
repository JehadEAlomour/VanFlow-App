package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.network.dto.StockRequestDto

/**
 * Asking the warehouse to load stock onto this van.
 *
 * The rep picks items and a quantity in whichever unit they think in — cartons,
 * usually — and the office decides how much to actually give. Approval does not
 * move stock: it moves when the rep confirms the goods are physically on the
 * van, which is why [StockRequestEvent.Receive] exists and why an approved
 * request stays on this screen until then.
 */
data class StockRequestLine(
    val product: Product,
    /** The unit asked for. Null means the item's base pieces. */
    val unit: ProductUnit? = null,
    val quantity: String = "",
) {
    val qtyOrZero: Double get() = quantity.toDoubleOrNull() ?: 0.0

    /** Pieces per chosen unit. 1 when asking in base pieces. */
    val factor: Int
        get() = unit?.conversionQty?.toInt()?.takeIf { it > 0 } ?: 1

    /** What the request will actually move, in stock-pool units. */
    val baseQty: Double get() = qtyOrZero * factor

    /** Stock already on the van for the pool this line draws on. */
    val onVan: Int
        get() = if (unit?.isStockUnit == true) unit.vanStock else product.vanStock
}

data class StockRequestState(
    val products: List<Product> = emptyList(),
    val units: List<ProductUnit> = emptyList(),
    val lines: List<StockRequestLine> = emptyList(),
    val note: String = "",

    /** Item picker sheet. */
    val isPickerOpen: Boolean = false,
    val searchQuery: String = "",
    /** Unit picker, keyed by the line it belongs to. Null when closed. */
    val unitPickerFor: Int? = null,

    val isSubmitting: Boolean = false,

    /**
     * This rep's recent requests. The screen is the rep's whole view of what
     * they have asked for, so it stays after submitting rather than navigating
     * away — the answer arrives here.
     */
    val mine: List<StockRequestDto> = emptyList(),
    val isLoadingMine: Boolean = false,
    /** Ids with a receive/cancel call in flight, so one row's spinner is its own. */
    val busyIds: Set<String> = emptySet(),

    val errorAr: String? = null,
    val noticeAr: String? = null,
) {
    val canSubmit: Boolean
        get() = lines.any { it.qtyOrZero > 0 } && !isSubmitting

    /** Units for one line's product, base first — the order the rep expects. */
    fun unitsFor(product: Product): List<ProductUnit> =
        units.filter { it.productId == product.id }.sortedBy { it.conversionQty }
}

sealed interface StockRequestEvent {
    data object OpenPicker : StockRequestEvent
    data object ClosePicker : StockRequestEvent
    data class SearchChanged(val v: String) : StockRequestEvent
    data class AddProduct(val product: Product) : StockRequestEvent
    data class QuantityChanged(val index: Int, val v: String) : StockRequestEvent
    data class RemoveLine(val index: Int) : StockRequestEvent
    data class NoteChanged(val v: String) : StockRequestEvent

    data class OpenUnitPicker(val index: Int) : StockRequestEvent
    data object CloseUnitPicker : StockRequestEvent
    data class UnitChanged(val index: Int, val unit: ProductUnit?) : StockRequestEvent

    /** Send the request to the office. */
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
