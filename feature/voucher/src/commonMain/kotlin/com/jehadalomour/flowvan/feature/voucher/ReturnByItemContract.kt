package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.network.dto.ReturnPlanDto

/**
 * Return by item on the van — returning goods without naming the sale.
 *
 * See cash-van-dashboard/docs/RETURNS-without-a-sale-voucher.md §3c. No strategy
 * picker: the app always matches NEWEST_FIRST. A salesman standing in a shop
 * should not be choosing an allocation policy, and the office can re-do the
 * return under a different order if the match is wrong.
 */
data class ReturnByItemLine(
    val product: Product,
    /** The unit coming back — null when the item has no unit rows. */
    val itemUnitId: String? = null,
    val unitLabel: String? = null,
    val quantity: String = "",
) {
    val qtyOrZero: Double get() = quantity.toDoubleOrNull() ?: 0.0
}

data class ReturnByItemState(
    val customerNumber: String? = null,
    val customerName: String? = null,
    val products: List<Product> = emptyList(),
    val lines: List<ReturnByItemLine> = emptyList(),

    /** Item picker sheet. */
    val isPickerOpen: Boolean = false,
    val searchQuery: String = "",

    val isPreviewing: Boolean = false,
    /** Null until the rep asks for a match. */
    val plan: ReturnPlanDto? = null,

    val isConfirming: Boolean = false,
    val createdVouchers: List<String> = emptyList(),
    val errorAr: String? = null,
) {
    val canPreview: Boolean
        get() = lines.any { it.qtyOrZero > 0 } && !isPreviewing && !isConfirming

    /**
     * Confirm is gated on a plan that actually matched something. A plan with
     * only unallocated lines would create nothing, and offering the button
     * anyway reads as "the system failed" rather than "these units are not
     * returnable".
     */
    val canConfirm: Boolean
        get() = (plan?.lines?.isNotEmpty() == true) && !isConfirming && createdVouchers.isEmpty()

    val hasUnmatched: Boolean get() = plan?.unallocated?.isNotEmpty() == true
}

sealed interface ReturnByItemEvent {
    data object OpenPicker : ReturnByItemEvent
    data object ClosePicker : ReturnByItemEvent
    data class SearchChanged(val v: String) : ReturnByItemEvent
    data class AddProduct(val product: Product) : ReturnByItemEvent
    data class QuantityChanged(val index: Int, val v: String) : ReturnByItemEvent
    data class UnitChanged(val index: Int, val itemUnitId: String?, val label: String?) :
        ReturnByItemEvent
    data class RemoveLine(val index: Int) : ReturnByItemEvent

    /** Ask the server which sales these units came from. Creates nothing. */
    data object Preview : ReturnByItemEvent

    /** Create the return vouchers — one per source sale. */
    data object Confirm : ReturnByItemEvent

    data object DismissError : ReturnByItemEvent
}
