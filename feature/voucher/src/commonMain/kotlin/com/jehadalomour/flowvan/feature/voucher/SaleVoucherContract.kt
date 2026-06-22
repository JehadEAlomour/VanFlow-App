package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.AppliedOffer
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.OfferChoice
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit

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
    // ── Offers (server-authoritative, display only) ───────────────────────────
    val appliedOffers: List<AppliedOffer> = emptyList(),
    val freeLines: List<FreeLine> = emptyList(),
    val offerInvoiceDiscount: Double = 0.0,
    val pendingChoices: List<OfferChoice> = emptyList(),
    val isEvaluatingOffers: Boolean = false,
    /**
     * Flat list of item numbers the rep picked as GIFTs for ITEM_QTY_REWARD offers.
     * Sent to the server on both evaluate and sale-upload so it adds the free lines.
     * A single offer can require N gifts, so the same offer may contribute N entries.
     */
    val chosenFreeItems: List<String> = emptyList(),
    /** itemNumber → offer line-discount amount (JOD), overlaid on matching cart lines. */
    val offerLineDiscounts: Map<String, Double> = emptyMap(),
) {
    val subtotal: Double get() = cart.sumOf { it.grossLineTotal }
    val lineDiscountTotal: Double get() = cart.sumOf { it.lineDiscount }
    val taxAmount: Double get() = cart.sumOf { it.lineTax }

    /** Server-driven per-line offer discounts (display overlay, on top of manual line discounts). */
    val offerLineDiscountTotal: Double get() = offerLineDiscounts.values.sum()

    val voucherDiscountAmount: Double get() {
        val afterLines = subtotal - lineDiscountTotal
        val input = voucherDiscountInput.toDoubleOrNull() ?: 0.0
        return when (voucherDiscountType) {
            DiscountType.PERCENT -> (afterLines * (input / 100.0)).coerceIn(0.0, afterLines)
            DiscountType.VALUE   -> input.coerceIn(0.0, afterLines)
        }
    }

    val totalDiscount: Double get() =
        lineDiscountTotal + voucherDiscountAmount + offerLineDiscountTotal + offerInvoiceDiscount

    /**
     * Final total. Free lines net 0 (full price + 100% discount) so they don't move
     * the total. Offer discounts are subtracted; never below 0.
     */
    val total: Double get() = (subtotal - totalDiscount + taxAmount).coerceAtLeast(0.0)
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
    data class ChooseFreeItem(val offerId: String, val itemNumber: String) : SaleVoucherEvent
    data object DismissFreeItemSheet : SaleVoucherEvent
}
