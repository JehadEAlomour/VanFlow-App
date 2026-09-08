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

    // ── What the salesman is measured against, and paid on ───────────────────
    // Two independent optional targets: what to SELL, and what to COLLECT. Null
    // means no target was set for that one — which is not the same as a target
    // of zero, so the two must stay distinguishable all the way to the screen.
    val salesTargetFils: Double? = null,
    val collectionTargetFils: Double? = null,

    // What they actually did, in fils.
    val totalSalesFils: Double = 0.0,
    val collectedFils: Double = 0.0,

    // Achieved-vs-target on each. Null when that target is not set.
    val salesProgressPct: Int? = null,
    val collectionProgressPct: Int? = null,

    // ── What that earns, in fils ─────────────────────────────────────────────
    // Shown so a salesman can see the month adding up rather than being told a
    // single number at the end of it.
    val commissionOnCashFils: Double = 0.0,
    val commissionOnCreditFils: Double = 0.0,
    val commissionOnCollectionFils: Double = 0.0,
    val commissionTotalFils: Double = 0.0,
)
