package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.AppliedOffer
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.OfferChoice
import com.jehadalomour.flowvan.core.model.OfferTotals
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.ServerLine

enum class VoucherView { PICKER, CART }

/**
 * Percent vs fixed-value discount selector. The SALE flow no longer uses manual discounts
 * (offers are server-authoritative), but the other voucher flows (RETURN, etc.) in this
 * package still depend on this enum, so it stays defined here.
 */
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
    /**
     * Whether the rep has chosen the payment method from the opening Cash/Credit chooser.
     * Gates the item picker — no items can be added until this is true. Stays editable
     * later via the small Cash/Credit toggle in the cart.
     */
    val paymentChosen: Boolean = false,
    val notes: String = "",
    val showSaveSheet: Boolean = false,
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
    // ── Offers (server-authoritative, display only) ───────────────────────────
    val appliedOffers: List<AppliedOffer> = emptyList(),
    val freeLines: List<FreeLine> = emptyList(),
    val pendingChoices: List<OfferChoice> = emptyList(),
    val isEvaluatingOffers: Boolean = false,
    /**
     * Flat list of item numbers the rep picked as GIFTs for ITEM_QTY_REWARD offers.
     * Sent to the server on both evaluate and sale-upload so it adds the free lines.
     * A single offer can require N gifts, so the same offer may contribute N entries.
     */
    val chosenFreeItems: List<String> = emptyList(),
    // ── Server-fed cart (online) ──────────────────────────────────────────────
    /**
     * True when the displayed cart + totals come from the server's `/offers/evaluate`
     * result (online). False → fall back to on-device [InvoiceTaxCalculator] (offline,
     * no offers). Drives which numbers the UI shows.
     */
    val offersFromServer: Boolean = false,
    /** The server's authoritative per-line result, keyed by itemNumber (= sku). */
    val serverLines: List<ServerLine> = emptyList(),
    /** The server's authoritative invoice totals. */
    val serverTotals: OfferTotals = OfferTotals.ZERO,
) {
    /** Offline = we have a non-empty cart but no fresh server result to display. */
    val isOffline: Boolean get() = cart.isNotEmpty() && !offersFromServer

    /** On-device calculation used as the offline fallback (no offers). */
    private val localSummary get() = InvoiceTaxCalculator.calculateInvoice(cart)

    val subtotal: Double get() =
        if (offersFromServer) serverTotals.subtotalJod else cart.sumOf { it.grossLineTotal }

    /** Total discount applied (line + invoice offer discounts). */
    val totalDiscount: Double get() =
        if (offersFromServer) serverTotals.totalDiscountJod
        else localSummary.totalLineDiscounts + localSummary.invoiceDiscountAmount

    val taxAmount: Double get() =
        if (offersFromServer) serverTotals.taxJod else localSummary.totalTax

    /**
     * Final total. Server value when online; on-device calc when offline. Free lines net 0
     * (full price + 100% discount) so they don't move the total. Never below 0.
     */
    val total: Double get() =
        if (offersFromServer) serverTotals.grandTotalJod
        else localSummary.grandTotal.coerceAtLeast(0.0)
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
    ) : SaleVoucherEvent
    data class ChangeQty(val productId: String, val qty: Double) : SaleVoucherEvent
    data class RemoveLine(val productId: String) : SaleVoucherEvent
    /** From the opening Cash/Credit chooser (and the editable in-cart toggle). */
    data class PaymentMethodChosen(val method: PaymentMethod) : SaleVoucherEvent
    /** Change the payment method later without re-gating the picker. */
    data class PaymentMethodSelected(val method: PaymentMethod) : SaleVoucherEvent
    data class NotesChanged(val notes: String) : SaleVoucherEvent
    data object ToggleView : SaleVoucherEvent
    data object OpenSaveSheet : SaleVoucherEvent
    data object DismissSaveSheet : SaleVoucherEvent
    data object ConfirmSave : SaleVoucherEvent
    data object DismissError : SaleVoucherEvent
    data class ChooseFreeItem(val offerId: String, val itemNumber: String) : SaleVoucherEvent
    data object DismissFreeItemSheet : SaleVoucherEvent
}
