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

    /**
     * The two things a salesman is measured on, in JOD major units.
     *
     * `null` means no target was set for that one — deliberately not zero, which
     * would read on screen as a target of nothing that has been perfectly
     * missed. A salesman may be given one, both, or neither.
     */
    val salesTarget: Double? = null,
    val collectionTarget: Double? = null,

    /** What they actually sold and collected this month, JOD major. */
    val salesAchieved: Double = 0.0,
    val collectedAchieved: Double = 0.0,

    /** Achieved-vs-target on each. Null when that target is not set. */
    val salesProgressPct: Int? = null,
    val collectionProgressPct: Int? = null,

    /**
     * What the month has earned, JOD major, broken into its parts.
     *
     * Shown in pieces rather than as one figure: a salesman who can see the
     * month adding up does not have to take the total on trust at the end of it.
     */
    val commissionOnCash: Double = 0.0,
    val commissionOnCredit: Double = 0.0,
    val commissionOnCollection: Double = 0.0,
    val commissionTotal: Double = 0.0,
)
