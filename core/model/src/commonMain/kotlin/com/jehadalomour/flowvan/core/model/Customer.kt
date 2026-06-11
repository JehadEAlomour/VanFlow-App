package com.jehadalomour.flowvan.shared.domain.model

enum class CustomerTier { A, B, C }

enum class CustomerSegment {
    CHAMPIONS,
    LOYAL,
    AT_RISK,
    PROMISING,
    DORMANT,
    REGULAR,
}

data class Customer(
    val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String?,
    val phone: String?,
    val area: String,
    val addressAr: String?,
    val tier: CustomerTier,
    val segment: CustomerSegment,
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