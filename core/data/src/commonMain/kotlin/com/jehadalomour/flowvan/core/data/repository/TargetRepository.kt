package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.model.SalesTarget
import com.jehadalomour.flowvan.core.network.api.TargetApi
import com.jehadalomour.flowvan.core.network.dto.TargetDto

/** Read-only access to the signed-in salesman's targets (server-sourced, no local cache). */
class TargetRepository(private val api: TargetApi) {

    /** Target history (most-recent first). Index 0 is the current month. */
    suspend fun myHistory(months: Int = 6): List<SalesTarget> =
        api.history(months).map { it.toDomain() }
}

/** Map a server target row → domain. AMOUNT money is fils → JOD major (÷1000); QTY as-is. */
private fun TargetDto.toDomain(): SalesTarget {
    val isAmount = metric == "AMOUNT"
    fun conv(v: Double): Double = if (isAmount) v / 1000.0 else v
    val achievedRaw = if (isAmount) actualAmount else actualQty
    return SalesTarget(
        year = year ?: 0,
        month = month ?: 0,
        isAmount = isAmount,
        hasTarget = metric != null && targetValue != null,
        target = conv(targetValue ?: 0.0),
        achieved = conv(achievedRaw),
        remaining = conv(remaining ?: 0.0),
        progressPct = progressPct ?: 0,
    )
}
