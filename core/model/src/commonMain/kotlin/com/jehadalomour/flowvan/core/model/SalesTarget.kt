package com.jehadalomour.flowvan.core.model

/**
 * The salesman's target for one month. Money values ([target], [achieved], [remaining])
 * are already in JOD major units when [isAmount] is true (converted from the server's
 * fils), or whole units when the metric is QTY — so the UI formats amounts with formatJod
 * and shows a plain number for quantity.
 */
data class SalesTarget(
    val year: Int,
    val month: Int,
    val isAmount: Boolean,     // AMOUNT metric (money) vs QTY (units)
    val hasTarget: Boolean,    // false = no target was set for this month
    val target: Double,        // JOD major (AMOUNT) or units (QTY)
    val achieved: Double,
    val remaining: Double,
    val progressPct: Int,
)
