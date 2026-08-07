package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.VoucherSummary
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.return_reason_expired
import com.jehadalomour.flowvan.core.designsystem.resources.return_reason_damaged
import com.jehadalomour.flowvan.core.designsystem.resources.return_reason_wrong_order
import com.jehadalomour.flowvan.core.designsystem.resources.return_reason_other
import org.jetbrains.compose.resources.StringResource

// labelAr is the canonical value persisted/sent with the return; labelRes is for display only.
enum class ReturnReason(val labelAr: String, val labelRes: StringResource) {
    EXPIRED("منتهي الصلاحية", Res.string.return_reason_expired),
    DAMAGED("تالف", Res.string.return_reason_damaged),
    WRONG_ORDER("خطأ في الطلب", Res.string.return_reason_wrong_order),
    OTHER("آخر", Res.string.return_reason_other),
}

data class ReturnVoucherState(
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val view: VoucherView = VoucherView.PICKER,
    val searchQuery: String = "",
    val reason: ReturnReason? = null,
    val notes: String = "",
    val showSaveSheet: Boolean = false,
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    /**
     * Same money engine as the SALE cart, instead of the flat `subtotal × 0.16` that used
     * to live here. That hardcode was wrong twice over: it ignored each item's own rate,
     * and under INCLUSIVE pricing it ADDED tax to a price that already contained it, so a
     * return credited the customer more than the sale had charged.
     */
    private val summary: VoucherSummary get() = InvoiceTaxCalculator.calculateInvoice(cart = cart)
    val subtotal: Double get() = summary.displaySubtotal
    val taxAmount: Double get() = summary.totalTax
    val total: Double get() = summary.grandTotal
}

sealed interface ReturnVoucherEvent {
    data class SearchChanged(val q: String) : ReturnVoucherEvent
    data class AddToCart(val product: Product) : ReturnVoucherEvent
    /** [unitId] "" = the item's base pool; a line is identified by (productId, unitId). */
    data class ChangeQty(val productId: String, val unitId: String = "", val qty: Double) : ReturnVoucherEvent
    data class RemoveLine(val productId: String, val unitId: String = "") : ReturnVoucherEvent
    data class ReasonSelected(val reason: ReturnReason) : ReturnVoucherEvent
    data class NotesChanged(val notes: String) : ReturnVoucherEvent
    data object ToggleView : ReturnVoucherEvent
    data object OpenSaveSheet : ReturnVoucherEvent
    data object DismissSaveSheet : ReturnVoucherEvent
    data object ConfirmSave : ReturnVoucherEvent
    data object DismissError : ReturnVoucherEvent
}
