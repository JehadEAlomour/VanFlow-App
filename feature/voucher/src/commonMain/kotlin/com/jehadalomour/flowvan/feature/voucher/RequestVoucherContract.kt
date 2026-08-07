package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.VoucherSummary
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.feature.voucher.VoucherView

data class RequestVoucherState(
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val view: VoucherView = VoucherView.PICKER,
    val searchQuery: String = "",
    val expectedDeliveryAt: Long? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    /** Same money engine as the SALE cart — see the note in ReturnVoucherState. */
    private val summary: VoucherSummary get() = InvoiceTaxCalculator.calculateInvoice(cart = cart)
    val subtotal: Double get() = summary.displaySubtotal
    val taxAmount: Double get() = summary.totalTax
    val total: Double get() = summary.grandTotal
}

sealed interface RequestVoucherEvent {
    data class SearchChanged(val q: String) : RequestVoucherEvent
    data class AddToCart(val product: Product) : RequestVoucherEvent
    /** [unitId] "" = the item's base pool; a line is identified by (productId, unitId). */
    data class ChangeQty(val productId: String, val unitId: String = "", val qty: Double) : RequestVoucherEvent
    data class RemoveLine(val productId: String, val unitId: String = "") : RequestVoucherEvent
    data class ExpectedDateChanged(val epochMillis: Long?) : RequestVoucherEvent
    data class NotesChanged(val notes: String) : RequestVoucherEvent
    data object ToggleView : RequestVoucherEvent
    data object Save : RequestVoucherEvent
    data object DismissError : RequestVoucherEvent
}
