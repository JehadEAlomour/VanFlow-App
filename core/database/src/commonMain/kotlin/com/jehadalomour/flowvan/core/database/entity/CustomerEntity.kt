package com.jehadalomour.flowvan.core.database.entity

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
)