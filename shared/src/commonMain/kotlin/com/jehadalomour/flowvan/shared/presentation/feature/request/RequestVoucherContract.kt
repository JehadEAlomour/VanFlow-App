package com.jehadalomour.flowvan.shared.presentation.feature.request

import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView

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
    val subtotal: Double get() = cart.sumOf { it.lineTotal }
    val taxAmount: Double get() = subtotal * 0.16
    val total: Double get() = subtotal + taxAmount
}

sealed interface RequestVoucherEvent {
    data class SearchChanged(val q: String) : RequestVoucherEvent
    data class AddToCart(val product: Product) : RequestVoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : RequestVoucherEvent
    data class RemoveLine(val productId: String) : RequestVoucherEvent
    data class ExpectedDateChanged(val epochMillis: Long?) : RequestVoucherEvent
    data class NotesChanged(val notes: String) : RequestVoucherEvent
    data object ToggleView : RequestVoucherEvent
    data object Save : RequestVoucherEvent
    data object DismissError : RequestVoucherEvent
}
