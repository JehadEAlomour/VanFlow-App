package com.jehadalomour.flowvan.shared.presentation.feature.returns

import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView

enum class ReturnReason(val labelAr: String) {
    EXPIRED("منتهي الصلاحية"),
    DAMAGED("تالف"),
    WRONG_ORDER("خطأ في الطلب"),
    OTHER("آخر"),
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
    val subtotal: Double get() = cart.sumOf { it.lineTotal }
    val taxAmount: Double get() = subtotal * 0.16
    val total: Double get() = subtotal + taxAmount
}

sealed interface ReturnVoucherEvent {
    data class SearchChanged(val q: String) : ReturnVoucherEvent
    data class AddToCart(val product: Product) : ReturnVoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : ReturnVoucherEvent
    data class RemoveLine(val productId: String) : ReturnVoucherEvent
    data class ReasonSelected(val reason: ReturnReason) : ReturnVoucherEvent
    data class NotesChanged(val notes: String) : ReturnVoucherEvent
    data object ToggleView : ReturnVoucherEvent
    data object OpenSaveSheet : ReturnVoucherEvent
    data object DismissSaveSheet : ReturnVoucherEvent
    data object ConfirmSave : ReturnVoucherEvent
    data object DismissError : ReturnVoucherEvent
}
