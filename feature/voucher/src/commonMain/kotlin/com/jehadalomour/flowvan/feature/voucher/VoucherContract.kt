package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.LineTaxType
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.VoucherSummary
import com.jehadalomour.flowvan.feature.voucher.ReturnReason
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView

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
    val savedId: String? = null,
    val errorAr: String? = null,
    /** Driven from AppSettings — stamps each new CartLine at add-time. */
    val taxType: LineTaxType = LineTaxType.TAXABLE,

    // ── RETURN: source sale invoice this return is issued against ──────────────
    /** The customer's confirmed SALE invoices, offered as return sources. */
    val sourceInvoices: List<InvoiceEntity> = emptyList(),
    val referenceInvoiceId: String? = null,
    val referenceNumber: String? = null,
    val showSourcePicker: Boolean = false,
    /** Max returnable qty per product (base units) = what was sold on the source invoice. */
    val soldQtyByProduct: Map<String, Double> = emptyMap(),
) {
    /** Full invoice calculation via the tax calculator (pure, no side effects). */
    val summary: VoucherSummary get() = InvoiceTaxCalculator.calculateInvoice(
        cart = cart,
        invoiceDiscount = voucherDiscountInput.toDoubleOrNull()?.let { v ->
            when (voucherDiscountType) {
                DiscountType.PERCENT -> InvoiceDiscountInput.Percent(v)
                DiscountType.VALUE   -> InvoiceDiscountInput.Fixed(v)
            }
        } ?: InvoiceDiscountInput.None,
    )

    val subtotal: Double get() = summary.subtotalBeforeDiscounts
    val lineDiscountTotal: Double get() = summary.totalLineDiscounts
    val taxAmount: Double get() = summary.totalTax
    val voucherDiscountAmount: Double get() = summary.invoiceDiscountAmount
    val totalDiscount: Double get() = summary.totalLineDiscounts + summary.invoiceDiscountAmount
    val total: Double get() = summary.grandTotal

    /** Label shown next to the tax row — clarifies inclusive vs additive. */
    val taxLabelAr: String get() = when (taxType) {
        LineTaxType.INCLUSIVE -> "الضريبة (مضمّنة)"
        LineTaxType.TAXABLE   -> "الضريبة"
        LineTaxType.EXEMPT    -> "الضريبة"
    }

    val canSave: Boolean get() = cart.isNotEmpty() && !isSaving &&
        (type != VoucherType.RETURN || (reason != null && referenceInvoiceId != null))

    /** RETURN must be issued against a real sale invoice of the same customer. */
    val requiresSourceInvoice: Boolean get() = type == VoucherType.RETURN

    // ── UI helpers — screen reads these, no when(type) logic in the screen ──
    val titleAr: String get() = when (type) {
        VoucherType.SALE   -> "فاتورة بيع"
        VoucherType.RETURN -> "فاتورة مرتجع"
        VoucherType.ORDER  -> "طلب مسبق"
    }
    val saveLabelAr: String get() = when (type) {
        VoucherType.SALE   -> "حفظ الفاتورة"
        VoucherType.RETURN -> "حفظ المرتجع"
        VoucherType.ORDER  -> "حفظ الطلب"
    }
    val confirmTextAr: String get() = when (type) {
        VoucherType.SALE   -> ""
        VoucherType.RETURN -> "سيتم تسجيل المرتجع وإعادة المخزون وتعديل الرصيد"
        VoucherType.ORDER  -> "سيتم تسجيل الطلب للمراجعة"
    }
    val deliveryDate: Long? = null
    val showDeliveryDate: Boolean get() = type == VoucherType.ORDER

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
    data object Save : VoucherEvent
    data object ConfirmSave : VoucherEvent
    data object DismissSaveSheet : VoucherEvent
    data class ReasonSelected(val reason: ReturnReason) : VoucherEvent
    data object DismissError : VoucherEvent

    // RETURN: choosing the source sale invoice
    data object OpenSourcePicker : VoucherEvent
    data object DismissSourcePicker : VoucherEvent
    data class SelectSourceInvoice(val invoiceId: String) : VoucherEvent
}
