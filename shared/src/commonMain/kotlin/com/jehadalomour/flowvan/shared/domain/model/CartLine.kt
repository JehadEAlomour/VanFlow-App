package com.jehadalomour.flowvan.shared.domain.model

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
) {
    val grossLineTotal: Double get() = unitPrice * qty
    val lineDiscount: Double get() = grossLineTotal * discountPct.coerceIn(0.0, 1.0)
    val lineTotal: Double get() = grossLineTotal - lineDiscount
    val lineTax: Double get() = lineTotal * taxRate
    /** Base-unit equivalent qty for stock deduction */
    val stockQty: Double get() = qty * unitConversionQty
}
