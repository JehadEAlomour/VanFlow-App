package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * A salesman target row from `GET /targets/me` / `GET /targets/me/history`.
 * Money fields (targetValue, actualAmount, remaining) are in FILS when metric = AMOUNT.
 */
@Serializable
data class TargetDto(
    val metric: String? = null,          // "AMOUNT" | "QTY" | null (no target set)
    val targetValue: Double? = null,     // fils (AMOUNT) or units (QTY)
    val actualAmount: Double = 0.0,      // fils
    val actualQty: Double = 0.0,         // units
    val progressPct: Int? = null,
    val remaining: Double? = null,       // target − actual (fils AMOUNT / units QTY)
    val year: Int? = null,               // present on /history rows
    val month: Int? = null,
)
