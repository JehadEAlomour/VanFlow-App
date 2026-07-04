package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.database.entity.OfferEntity
import com.jehadalomour.flowvan.core.network.dto.OfferDto

/**
 * Maps a GET /offers/active [OfferDto] into the cache [OfferEntity]. The type-specific
 * `trigger`/`reward`/`eligibility` objects are kept as their raw JSON text (JsonObject
 * `.toString()` is valid JSON) and parsed on read by the offline evaluator.
 */
fun OfferDto.toEntity(cachedAt: Long): OfferEntity = OfferEntity(
    id = id,
    name = name,
    description = description,
    type = type,
    triggerJson = trigger?.toString() ?: "{}",
    rewardJson = reward?.toString() ?: "{}",
    eligibilityJson = eligibility?.toString() ?: "{}",
    validFrom = validFrom,
    validTo = validTo,
    daysOfWeekCsv = daysOfWeek?.joinToString(","),
    timeFrom = timeFrom,
    timeTo = timeTo,
    totalRedemptionLimit = totalRedemptionLimit,
    perCustomerLimit = perCustomerLimit,
    priority = priority,
    stackable = stackable,
    isActive = isActive,
    redemptionCount = redemptionCount,
    createdAt = createdAt,
    cachedAt = cachedAt,
)
