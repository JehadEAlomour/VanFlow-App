package com.jehadalomour.flowvan.core.model

data class CartLine(
    val productId: String,
    val sku: String,
    val nameAr: String,
    val unitPrice: Double,
    val qty: Double,
    val discountPct: Double = 0.0,
    val unit: String = "",
    val unitConversionQty: Double = 1.0,
    val taxRate: Double = 0.16,
    val lineTaxType: LineTaxType = LineTaxType.TAXABLE,
    /** Product image (carried from the product) so cart rows show the image too. */
    val imageUrl: String? = null,
    // ── Tobacco tax — set only when the feature flag is ON and the item is tobacco.
    /** When true this line is taxed by [tobaccoProfile] instead of normal GST. */
    val isTobacco: Boolean = false,
    /** The tobacco profile applied to this line (null → normal GST). */
    val tobaccoProfile: TobaccoTaxProfile? = null,
    /** MSRP / consumer price for the tobacco base, integer fils per base piece. */
    val consumerPriceFils: Long = 0L,
) {
    /** qty × unitPrice — gross before any discount. */
    val grossLineTotal: Double get() = unitPrice * qty

    /** Line-level discount amount (clamped to [0, gross]). */
    val lineDiscount: Double get() = grossLineTotal * discountPct.coerceIn(0.0, 1.0)

    /** Net after line discount, before tax. Used by the invoice calculator. */
    val lineNet: Double get() = grossLineTotal - lineDiscount

    /**
     * Tax amount on this line (before invoice-level discount).
     *
     * TAXABLE   → lineNet × taxRate         (tax added on top)
     * INCLUSIVE → lineNet × r / (1 + r)     (tax extracted from price)
     * EXEMPT    → 0
     */
    val lineTax: Double get() = when (lineTaxType) {
        LineTaxType.TAXABLE   -> lineNet * taxRate
        LineTaxType.INCLUSIVE -> lineNet * (taxRate / (1.0 + taxRate))
        LineTaxType.EXEMPT    -> 0.0
    }

    /**
     * What the customer pays for this line (before invoice-level discount).
     *
     * TAXABLE   → lineNet + lineTax  (tax added on top)
     * INCLUSIVE → lineNet            (tax is already inside the price)
     * EXEMPT    → lineNet
     */
    val lineTotal: Double get() = when (lineTaxType) {
        LineTaxType.TAXABLE   -> lineNet + lineTax
        LineTaxType.INCLUSIVE -> lineNet
        LineTaxType.EXEMPT    -> lineNet
    }

    /** Base-unit equivalent qty for stock deduction. */
    val stockQty: Double get() = qty * unitConversionQty
}
