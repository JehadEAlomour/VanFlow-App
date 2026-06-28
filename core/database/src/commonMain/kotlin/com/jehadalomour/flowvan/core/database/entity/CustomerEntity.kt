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
)