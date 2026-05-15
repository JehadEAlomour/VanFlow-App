package com.jehadalomour.flowvan.shared.presentation.feature.sale

import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.domain.model.Product

enum class VoucherView { PICKER, CART }

data class SaleVoucherState(
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val view: VoucherView = VoucherView.PICKER,
    val searchQuery: String = "",
    val discountAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val showSaveSheet: Boolean = false,
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    val subtotal: Double get() = cart.sumOf { it.lineTotal }
    val taxBase: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount: Double get() = taxBase * 0.16
    val total: Double get() = taxBase + taxAmount
}

sealed interface SaleVoucherEvent {
    data class SearchChanged(val q: String) : SaleVoucherEvent
    data class AddToCart(val product: Product) : SaleVoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : SaleVoucherEvent
    data class RemoveLine(val productId: String) : SaleVoucherEvent
    data class DiscountChanged(val amount: Double) : SaleVoucherEvent
    data class PaymentMethodSelected(val method: PaymentMethod) : SaleVoucherEvent
    data class NotesChanged(val notes: String) : SaleVoucherEvent
    data object ToggleView : SaleVoucherEvent
    data object OpenSaveSheet : SaleVoucherEvent
    data object DismissSaveSheet : SaleVoucherEvent
    data object ConfirmSave : SaleVoucherEvent
    data object DismissError : SaleVoucherEvent
}
