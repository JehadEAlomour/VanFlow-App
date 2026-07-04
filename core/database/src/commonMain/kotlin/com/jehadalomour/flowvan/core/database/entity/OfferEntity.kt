package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached offer definition for offline evaluation (from GET /offers/active). The
 * type-specific `trigger`/`reward`/`eligibility` blobs are stored as raw JSON text and
 * parsed on read. [createdAt] preserves the server's priority-tie ordering; [cachedAt] is
 * the epoch-ms of the refresh that wrote the row.
 */
@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val triggerJson: String,
    val rewardJson: String,
    val eligibilityJson: String,
    val validFrom: String?,
    val validTo: String?,
    /** Comma-joined weekday numbers (0=Sun..6=Sat), e.g. "0,3,6". Null = every day. */
    val daysOfWeekCsv: String?,
    val timeFrom: String?,
    val timeTo: String?,
    val totalRedemptionLimit: Int?,
    val perCustomerLimit: Int?,
    val priority: Int,
    val stackable: Boolean,
    val isActive: Boolean,
    val redemptionCount: Int,
    /** Server creation instant (ISO-8601) — preserves priority-tie order. */
    val createdAt: String?,
    /** Epoch-ms of the refresh that cached this row (drives last-refresh reporting). */
    val cachedAt: Long,
)
