package com.jehadalomour.flowvan.shared.presentation.feature.sale

import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.domain.model.ProductUnit

enum class VoucherView { PICKER, CART }
enum class DiscountType { PERCENT, VALUE }

data class SaleVoucherState(
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val productUnits: Map<String, List<ProductUnit>> = emptyMap(),
    val view: VoucherView = VoucherView.PICKER,
    val searchQuery: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val showSaveSheet: Boolean = false,
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
    val voucherDiscountType: DiscountType = DiscountType.PERCENT,
    val voucherDiscountInput: String = "",
) {
    val subtotal: Double get() = cart.sumOf { it.grossLineTotal }
    val lineDiscountTotal: Double get() = cart.sumOf { it.lineDiscount }
    val taxAmount: Double get() = cart.sumOf { it.lineTax }

    val voucherDiscountAmount: Double get() {
        val afterLines = subtotal - lineDiscountTotal
        val input = voucherDiscountInput.toDoubleOrNull() ?: 0.0
        return when (voucherDiscountType) {
            DiscountType.PERCENT -> (afterLines * (input / 100.0)).coerceIn(0.0, afterLines)
            DiscountType.VALUE   -> input.coerceIn(0.0, afterLines)
        }
    }

    val totalDiscount: Double get() = lineDiscountTotal + voucherDiscountAmount
    val total: Double get() = subtotal - totalDiscount + taxAmount
}

sealed interface SaleVoucherEvent {
    data class SearchChanged(val q: String) : SaleVoucherEvent
    data class StepItem(val product: Product, val delta: Int) : SaleVoucherEvent
    data class ConfirmItemDialog(
        val product: Product,
        val qty: Double,
        val unit: String,
        val unitPrice: Double,
        val unitConversionQty: Double,
        val discountPct: Double,
    ) : SaleVoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : SaleVoucherEvent
    data class RemoveLine(val productId: String) : SaleVoucherEvent
    data class PaymentMethodSelected(val method: PaymentMethod) : SaleVoucherEvent
    data class NotesChanged(val notes: String) : SaleVoucherEvent
    data class VoucherDiscountInputChanged(val input: String) : SaleVoucherEvent
    data object VoucherDiscountTypeToggled : SaleVoucherEvent
    data object ToggleView : SaleVoucherEvent
    data object OpenSaveSheet : SaleVoucherEvent
    data object DismissSaveSheet : SaleVoucherEvent
    data object ConfirmSave : SaleVoucherEvent
    data object DismissError : SaleVoucherEvent
}
