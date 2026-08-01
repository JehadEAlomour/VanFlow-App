package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.model.AppliedOffer
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.InvoiceAppliedOffer
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.LineTaxType
import com.jehadalomour.flowvan.core.model.OfferChoice
import com.jehadalomour.flowvan.core.model.OfferSource
import com.jehadalomour.flowvan.core.model.OfferTotals
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.TobaccoTaxProfile
import com.jehadalomour.flowvan.core.model.ServerLine
import com.jehadalomour.flowvan.core.model.VoucherSummary
import com.jehadalomour.flowvan.feature.voucher.ReturnReason
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.StringResource

enum class VoucherView { PICKER, CART }

/**
 * Percent vs fixed-value discount selector. The SALE flow no longer uses manual discounts
 * (offers are server-authoritative), but the other voucher flows (RETURN, etc.) in this
 * package still depend on this enum, so it stays defined here.
 */
enum class DiscountType { PERCENT, VALUE }

data class VoucherState(
    val type: VoucherType,
    val customer: Customer? = null,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val productUnits: Map<String, List<ProductUnit>> = emptyMap(),
    /** Synced tobacco tax profiles by id — resolved when a tobacco item is added. */
    val tobaccoProfiles: Map<String, TobaccoTaxProfile> = emptyMap(),
    /** The selected customer's price list as sku→base-unit price (JOD). Empty when
     *  unassigned; an item present here prices at the list price, else the base price. */
    val customerPrices: Map<String, Double> = emptyMap(),
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
    val savedId: String? = null,
    val errorAr: String? = null,
    /** Driven from AppSettings — stamps each new CartLine at add-time. */
    val taxType: LineTaxType = LineTaxType.TAXABLE,
    /**
     * Admin switch (`vouchers.discount.direct`): show the discount fields at all.
     *
     * UI-only. The server accepts a discount from any rep — it no longer rejects
     * one or routes it through approval — so this controls what the salesman can
     * SEE, not what the backend will take. Default false so a session that has
     * not loaded permissions yet doesn't flash the field on.
     */
    val canDiscount: Boolean = false,
    /** Salesman permission: may change an item's unit price when selling. */
    val canEditPrice: Boolean = false,
    /** Salesman permission: may create returns at all. */
    val canCreateReturn: Boolean = false,
    /** Salesman permission: returns require admin approval (Save → Request approval). */
    val returnNeedsApproval: Boolean = false,
    /** A return approval request is pending a manager decision — cart is blocked from print. */
    val pendingApprovalId: String? = null,
    /** Manager's note when a request was rejected (shown to the salesman). */
    val approvalDecisionNote: String? = null,

    // ── RETURN: source sale invoice this return is issued against ──────────────
    /** The customer's confirmed SALE invoices, offered as return sources. */
    val sourceInvoices: List<InvoiceEntity> = emptyList(),
    val referenceInvoiceId: String? = null,
    val referenceNumber: String? = null,
    val showSourcePicker: Boolean = false,
    /** Manual lookup box: find a sale by number on the server when it isn't local. */
    val sourceLookupQuery: String = "",
    val isLookingUp: Boolean = false,
    /** Max returnable qty per product (base units) = what was sold on the source invoice. */
    val soldQtyByProduct: Map<String, Double> = emptyMap(),

    // ── Offers (SALE only — server-authoritative, display only) ────────────────
    /**
     * Whether the rep has chosen the payment method from the opening Cash/Credit chooser.
     * SALE only — gates the picker until chosen. Not applicable to RETURN/ORDER.
     */
    val paymentChosen: Boolean = false,
    /** Offers that were applied — drives the banner chips (SALE). */
    val appliedOffers: List<AppliedOffer> = emptyList(),
    /** Free items the offers add as net-0 lines (SALE). */
    val freeLines: List<FreeLine> = emptyList(),
    /** Offers awaiting a free-item pick — drives the choose-free-item sheet (SALE). */
    val pendingChoices: List<OfferChoice> = emptyList(),
    /**
     * Flat list of item numbers the rep picked as GIFTs for ITEM_QTY_REWARD offers.
     * Sent to the server on both evaluate and sale-upload so it adds the free lines (SALE).
     */
    val chosenFreeItems: List<String> = emptyList(),
    /**
     * Where the current offer evaluation came from (SALE): SERVER (online /offers/evaluate),
     * LOCAL (offline on-device evaluator over the cached offers), or null (no evaluation yet /
     * both failed → on-device totals with no offers).
     */
    val offersSource: OfferSource? = null,
    /** The server's authoritative per-line result, keyed by itemNumber (= sku) (SALE). */
    val serverLines: List<ServerLine> = emptyList(),
    /** The server's authoritative invoice totals (SALE). */
    val serverTotals: OfferTotals = OfferTotals.ZERO,
    /** True while a debounced offer evaluation is in flight (SALE). */
    val isEvaluatingOffers: Boolean = false,
) {
    /** The voucher-footer discount the rep entered, parsed into the calculator input. */
    private val invoiceDiscountInputParsed: InvoiceDiscountInput get() =
        voucherDiscountInput.toDoubleOrNull()?.let { v ->
            when (voucherDiscountType) {
                DiscountType.PERCENT -> InvoiceDiscountInput.Percent(v)
                DiscountType.VALUE   -> InvoiceDiscountInput.Fixed(v)
            }
        } ?: InvoiceDiscountInput.None

    /** Full invoice calculation via the tax calculator (pure, no side effects). */
    val summary: VoucherSummary get() = InvoiceTaxCalculator.calculateInvoice(
        cart = cart,
        invoiceDiscount = invoiceDiscountInputParsed,
    )

    /**
     * Cart lines with the offer engine's per-line discount overlaid on top of any manual
     * line discount (keyed by sku). Display-only — the raw cart is uploaded and the server
     * re-applies offers. Gift free-lines are net-0 so they don't affect the totals below.
     */
    private val offerAdjustedCart: List<CartLine> get() {
        val bySku = serverLines.associateBy { it.itemNumber }
        return cart.map { line ->
            val frac = bySku[line.sku]?.discountFraction ?: 0.0
            if (frac <= 0.0) line
            else line.copy(discountPct = (line.discountPct + frac).coerceIn(0.0, 1.0))
        }
    }

    /**
     * The cart to PERSIST on save: offer discounts baked into each line so the saved and
     * printed invoice matches the displayed totals (correct even offline). The raw [cart]
     * when no offer evaluation is active. The create use case still uploads the raw cart —
     * the server re-applies offers, so this only drives the stored/display invoice.
     */
    val displayCart: List<CartLine> get() = if (useServerOffers) offerAdjustedCart else cart

    /**
     * Per-offer discount breakdown for the printed receipt (SALE only): each applied offer's
     * TOTAL discount value (JOD), summed from its per-line shares ([ServerLine.offers]) plus any
     * gift/free-line value ([freeLines]). Frozen onto the invoice at save time so the printout can
     * itemize offers even when reprinted later from a report. Empty when no offers applied.
     */
    val printedOffers: List<InvoiceAppliedOffer> get() {
        if (type != VoucherType.SALE) return emptyList()
        val amountById = linkedMapOf<String, Double>()   // preserves offer order
        val nameById = mutableMapOf<String, String>()
        // Line-level offer discounts (percent / per-unit-amount rewards).
        serverLines.forEach { sl ->
            sl.offers.forEach { o ->
                amountById[o.offerId] = (amountById[o.offerId] ?: 0.0) + o.discountJod
                nameById[o.offerId] = o.name
            }
        }
        // Gift / free lines (ITEM_QTY_REWARD): the offer's discount is the gift's full value.
        freeLines.forEach { fl ->
            val v = fl.qty * fl.unitPriceJod
            if (v > 0.0) amountById[fl.offerId] = (amountById[fl.offerId] ?: 0.0) + v
        }
        // Fill any missing names from the applied-offer banner list.
        appliedOffers.forEach { ao -> nameById.getOrPut(ao.offerId) { ao.name } }
        return amountById.entries
            .filter { it.value > 0.0005 }
            .map { InvoiceAppliedOffer(name = nameById[it.key] ?: "عرض", discountAmount = it.value) }
    }

    /**
     * The summary that drives the displayed totals. When an offer evaluation is active we
     * STILL compute tax + grand total on-device through the tax-mode-aware calculator
     * (overlaying the engine's offer discounts), because the offers engine computes tax
     * EXCLUDED-only — trusting its totals would add tax on top even for INCLUSIVE-priced
     * companies. This mirrors the authoritative backend posting (same fils calc + tax mode).
     */
    val displaySummary: VoucherSummary get() =
        if (useServerOffers) InvoiceTaxCalculator.calculateInvoice(
            cart = offerAdjustedCart,
            invoiceDiscount = invoiceDiscountInputParsed,
        ) else summary

    /**
     * SALE only: an offer evaluation (server OR offline) produced per-line results, so
     * totals/lines are fed from [serverLines]/[serverTotals]. Requires [serverLines] to be
     * non-empty — otherwise (e.g. offline with an empty offers cache) totals fall back to the
     * on-device [summary] instead of showing the evaluator's zero totals.
     */
    val useServerOffers: Boolean get() =
        type == VoucherType.SALE && offersSource != null && serverLines.isNotEmpty()

    /**
     * SALE only: a non-empty cart whose evaluation isn't a live result → offline banner.
     * SERVER (online) and LOCAL_CONTRACT (on-device on purpose, to honor the customer's price
     * list) are both "live"; only a genuine offline/failed evaluation shows the banner.
     */
    val isOffline: Boolean get() = type == VoucherType.SALE && cart.isNotEmpty() &&
        offersSource != OfferSource.SERVER && offersSource != OfferSource.LOCAL_CONTRACT

    /** SALE offline banner copy — distinguishes "offers from local cache" from "no offers". */
    val offlineBannerTextAr: String get() = when (offersSource) {
        OfferSource.LOCAL -> "غير متصل — العروض من الذاكرة المحلية، يتحقق الخادم عند المزامنة"
        else -> "غير متصل — الإجماليات محلية، يعاد تطبيق العروض عند المزامنة"
    }

    val subtotal: Double get() = displaySummary.subtotalBeforeDiscounts
    val lineDiscountTotal: Double get() = displaySummary.totalLineDiscounts
    val taxAmount: Double get() = displaySummary.totalTax
    /** Manual voucher-level discount the rep entered (drives the SAVE); offers are separate. */
    val voucherDiscountAmount: Double get() = summary.invoiceDiscountAmount
    val totalDiscount: Double get() =
        displaySummary.totalLineDiscounts + displaySummary.invoiceDiscountAmount
    val total: Double get() = displaySummary.grandTotal

    /** Label shown next to the tax row — clarifies inclusive vs additive. */
    val taxLabelRes: StringResource get() = when (taxType) {
        LineTaxType.INCLUSIVE -> Res.string.tax_label_inclusive
        LineTaxType.TAXABLE   -> Res.string.tax_label_default
        LineTaxType.EXEMPT    -> Res.string.tax_label_default
    }

    /** True while a return request is awaiting a manager decision — the cart is locked. */
    val isAwaitingApproval: Boolean get() = pendingApprovalId != null

    /** Any discount present — on a line or voucher-level. */
    val hasDiscount: Boolean get() =
        cart.any { it.discountPct > 0.0 } || (voucherDiscountInput.toDoubleOrNull() ?: 0.0) > 0.0

    /** Discount inputs appear only when the admin enabled them for this rep. */
    val showDiscountInputs: Boolean get() = canDiscount

    /** No discount ever needs approval — the backend accepts them all. */
    val needsDiscountApproval: Boolean get() = false

    // A salesman may build a RETURN if they can create directly OR if returns need
    // approval (the button becomes "request approval"). Either flag enables it.
    // While a request is pending, the action is locked (waiting on the manager).
    val canSave: Boolean get() = cart.isNotEmpty() && !isSaving && !isAwaitingApproval &&
        (type != VoucherType.RETURN ||
            (reason != null && referenceInvoiceId != null && (canCreateReturn || returnNeedsApproval)))

    /** RETURN must be issued against a real sale invoice of the same customer. */
    val requiresSourceInvoice: Boolean get() = type == VoucherType.RETURN

    // ── UI helpers — screen reads these, no when(type) logic in the screen ──
    val titleRes: StringResource get() = when (type) {
        VoucherType.SALE   -> Res.string.voucher_title_sale
        VoucherType.RETURN -> Res.string.voucher_title_return
        VoucherType.ORDER  -> Res.string.voucher_title_order
    }
    val saveLabelRes: StringResource get() = when (type) {
        // A discounted SALE the salesman may only request → "request manager approval".
        VoucherType.SALE   -> if (needsDiscountApproval) Res.string.voucher_save_discount_approval else Res.string.voucher_save_sale
        // Returns that need approval: the button becomes "request manager approval".
        VoucherType.RETURN -> if (returnNeedsApproval) Res.string.voucher_save_return_approval else Res.string.voucher_save_return
        VoucherType.ORDER  -> Res.string.voucher_save_order
    }
    /** Confirm-dialog body; null for SALE (uses the payment dialog instead). */
    val confirmTextRes: StringResource? get() = when (type) {
        VoucherType.SALE   -> null
        VoucherType.RETURN -> Res.string.voucher_confirm_return
        VoucherType.ORDER  -> Res.string.voucher_confirm_order
    }
    val deliveryDate: Long? = null
    val showDeliveryDate: Boolean get() = type == VoucherType.ORDER

    val showReasonRow: Boolean get() = type == VoucherType.RETURN
    val showStockBadge: Boolean get() = type == VoucherType.SALE

    /**
     * Manual invoice-level discount UI. Disabled for SALE — offers are the only discount
     * and are applied server-side (authoritative). RETURN/ORDER unchanged (they never
     * showed it; the [DiscountType] enum / voucher-discount state remain for the contract).
     */

    /** SALE only: show the opening blocking Cash/Credit chooser until chosen. */
    val showPaymentChooser: Boolean get() = type == VoucherType.SALE && !paymentChosen

    /** SALE only: offer banners / free-line UI are relevant. */
    val offersEnabled: Boolean get() = type == VoucherType.SALE
    val showDiscountSection: Boolean get() = type == VoucherType.SALE
    // SALE and RETURN both ask cash vs credit before saving — for a return the choice
    // drives the balance change (cash refund vs credit note). ORDER keeps a plain confirm.
    val showPaymentDialog: Boolean get() = type == VoucherType.SALE || type == VoucherType.RETURN
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
    /** SALE: from the opening Cash/Credit chooser — sets method AND unblocks the picker. */
    data class PaymentMethodChosen(val method: PaymentMethod) : VoucherEvent
    data class NotesChanged(val notes: String) : VoucherEvent
    data class VoucherDiscountInputChanged(val input: String) : VoucherEvent
    data object VoucherDiscountTypeToggled : VoucherEvent
    data object ToggleView : VoucherEvent
    data object Save : VoucherEvent
    data object ConfirmSave : VoucherEvent
    data object CancelApproval : VoucherEvent
    data object DismissSaveSheet : VoucherEvent
    data class ReasonSelected(val reason: ReturnReason) : VoucherEvent
    data object DismissError : VoucherEvent

    // RETURN: choosing the source sale invoice
    data object OpenSourcePicker : VoucherEvent
    data object DismissSourcePicker : VoucherEvent
    data class SelectSourceInvoice(val invoiceId: String) : VoucherEvent
    data class SourceLookupChanged(val q: String) : VoucherEvent
    data object LookupSource : VoucherEvent

    // SALE: offers — GIFT picks for ITEM_QTY_REWARD offers. Choose adds one (up to the
    // offer's quota), Remove takes one back. The same pool item can be picked multiple times.
    data class ChooseFreeItem(val offerId: String, val itemNumber: String) : VoucherEvent
    data class RemoveFreeItem(val offerId: String, val itemNumber: String) : VoucherEvent
    data object DismissFreeItemSheet : VoucherEvent
}
