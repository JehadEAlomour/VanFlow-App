package com.jehadalomour.flowvan.core.model

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
    /** Server category — drives SEGMENT offer eligibility (null until a catalog refresh fills it). */
    val category: String? = null,
    /** Server region id — drives regionIds offer eligibility. */
    val regionId: String? = null,
    /** Server rep id — repIds offer eligibility fallback (ctx.repId ?? customer.repId). */
    val repId: String? = null,
    /** Assigned price list id (price_lists.id). Null = base catalog prices. */
    val priceListId: String? = null,
)