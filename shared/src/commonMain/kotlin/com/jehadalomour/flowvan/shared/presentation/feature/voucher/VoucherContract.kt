package com.jehadalomour.flowvan.shared.presentation.feature.voucher

import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.domain.model.ProductUnit
import com.jehadalomour.flowvan.shared.presentation.feature.returns.ReturnReason
import com.jehadalomour.flowvan.shared.presentation.feature.sale.DiscountType
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView

data class VoucherState(
    val type: VoucherType,
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val productUnits: Map<String, List<ProductUnit>> = emptyMap(),
    val cart: List<CartLine> = emptyList(),
    val view: VoucherView = VoucherView.PICKER,
    val searchQuery: String = "",
    val reason: ReturnReason? = null,
    val notes: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val voucherDiscountType: DiscountType = DiscountType.PERCENT,
    val voucherDiscountInput: String = "",
    val showSaveSheet: Boolean = false,
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    val subtotal: Double get() = cart.sumOf { it.grossLineTotal }
    val lineDiscountTotal: Double get() = cart.sumOf { it.lineDiscount }
    val taxAmount: Double get() = cart.sumOf { it.lineTax }

    val voucherDiscountAmount: Double get() {
        val afterLines = subtotal - lineDiscountTotal
        val input = voucherDiscountInput.toDoubleOrNull() ?: 0.0
        return when (voucherDiscountType) {
            DiscountType.PERCENT -> (afterLines * (input / 100.0)).coerceIn(0.0, afterLines)
            DiscountType.VALUE -> input.coerceIn(0.0, afterLines)
        }
    }
    val totalDiscount: Double get() = lineDiscountTotal + voucherDiscountAmount
    val total: Double get() = subtotal - totalDiscount + taxAmount

    val canSave: Boolean get() = cart.isNotEmpty() && !isSaving &&
        (type != VoucherType.RETURN || reason != null)

    // UI helpers — screen reads these so it contains no when(type) logic for behavior
    val titleAr: String get() = when (type) {
        VoucherType.SALE -> "فاتورة بيع"
        VoucherType.RETURN -> "فاتورة مرتجع"
        VoucherType.ORDER -> "طلب مسبق"
    }
    val saveLabelAr: String get() = when (type) {
        VoucherType.SALE -> "حفظ الفاتورة"
        VoucherType.RETURN -> "حفظ المرتجع"
        VoucherType.ORDER -> "حفظ الطلب"
    }
    val confirmTextAr: String get() = when (type) {
        VoucherType.SALE -> ""  // SALE uses PaymentMethodDialog, not this text
        VoucherType.RETURN -> "سيتم تسجيل المرتجع وإعادة المخزون وتعديل الرصيد"
        VoucherType.ORDER -> "سيتم تسجيل الطلب للمراجعة"
    }
    val showReasonRow: Boolean get() = type == VoucherType.RETURN
    val showStockBadge: Boolean get() = type == VoucherType.SALE
    val showDiscountSection: Boolean get() = type == VoucherType.SALE
    val showPaymentDialog: Boolean get() = type == VoucherType.SALE
}

sealed interface VoucherEvent {
    data class SearchChanged(val q: String) : VoucherEvent
    data class StepItem(val product: Product, val delta: Int) : VoucherEvent
    data class ConfirmItemDialog(
        val product: Product,
        val qty: Double,
        val unit: String,
        val unitPrice: Double,
        val unitConversionQty: Double,
        val discountPct: Double,
    ) : VoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : VoucherEvent
    data class RemoveLine(val productId: String) : VoucherEvent
    data class PaymentMethodSelected(val method: PaymentMethod) : VoucherEvent
    data class NotesChanged(val notes: String) : VoucherEvent
    data class VoucherDiscountInputChanged(val input: String) : VoucherEvent
    data object VoucherDiscountTypeToggled : VoucherEvent
    data object ToggleView : VoucherEvent
    data object Save : VoucherEvent          // unified trigger — VM decides dialog vs direct
    data object ConfirmSave : VoucherEvent
    data object DismissSaveSheet : VoucherEvent
    data class ReasonSelected(val reason: ReturnReason) : VoucherEvent
    data object DismissError : VoucherEvent
}
