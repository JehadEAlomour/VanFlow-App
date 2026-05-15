package com.jehadalomour.flowvan.shared.domain.model

data class CartLine(
    val productId: String,
    val sku: String,
    val nameAr: String,
    val unitPrice: Double,
    val qty: Double,
    val discountPct: Double = 0.0,
) {
    val grossLineTotal: Double get() = unitPrice * qty
    val lineTotal: Double get() = grossLineTotal * (1 - discountPct.coerceIn(0.0, 1.0))
}
