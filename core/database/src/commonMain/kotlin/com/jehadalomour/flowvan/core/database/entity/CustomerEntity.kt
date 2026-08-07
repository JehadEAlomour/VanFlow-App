package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String?,
    val phone: String?,
    val area: String,
    val addressAr: String?,
    val tier: String,
    val segment: String,
    val churnRisk: Double,
    val balance: Double,
    val overdueAmount: Double,
    val creditLimit: Double,
    val taxNumber: String?,
    val isOnRoute: Boolean,
    val visitOrder: Int,
    val lat: Double?,
    val lng: Double?,
    /** Server category — SEGMENT offer eligibility. Null on rows cached before v8. */
    val category: String? = null,
    /** Server region id — regionIds offer eligibility. */
    val regionId: String? = null,
    /** Server rep id — repIds offer eligibility fallback. */
    val repId: String? = null,
    /** Assigned price list id (price_lists.id). Null = base catalog prices. */
    val priceListId: String? = null,
    // ── Tax exemption (v18) — mirrored from the server on every customer sync.
    // Cached because the rep sells offline: the cart has to know the sale will
    // be exempt without asking the server first.
    // defaultValue is required for the v17→v18 auto-migration: existing rows need
    // a value, and "not exempt" is the truthful answer for every one of them.
    @ColumnInfo(defaultValue = "0")
    val isTaxExempt: Boolean = false,
    val taxExemptionType: String? = null,
    val taxExemptionNumber: String? = null,
    val taxExemptionReason: String? = null,
    val taxExemptionValidFrom: Long? = null,
    val taxExemptionValidTo: Long? = null,
)