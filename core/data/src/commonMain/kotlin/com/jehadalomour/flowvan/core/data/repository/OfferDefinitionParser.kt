package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.entity.OfferEntity
import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.model.OfferEligibilityRule
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferTrigger
import com.jehadalomour.flowvan.core.model.OfferType
import com.jehadalomour.flowvan.core.model.TableEntry
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses a cached [OfferEntity] (raw JSON blobs) into an engine-ready [OfferDefinition].
 * Returns null for an unknown offer type / reward kind or malformed config, so a new
 * server-side type is skipped offline rather than crashing (the server still applies it
 * on sync).
 */
internal fun OfferEntity.toDefinition(json: Json): OfferDefinition? {
    val offerType = when (type) {
        "PAYMENT_METHOD_DISCOUNT" -> OfferType.PAYMENT_METHOD_DISCOUNT
        "ITEM_QTY_REWARD" -> OfferType.ITEM_QTY_REWARD
        else -> return null
    }
    val t = runCatching { json.decodeFromString<TriggerJson>(triggerJson) }.getOrNull() ?: return null
    val r = runCatching { json.decodeFromString<RewardJson>(rewardJson) }.getOrNull() ?: return null
    val e = runCatching { json.decodeFromString<EligibilityJson>(eligibilityJson) }.getOrNull()

    val trigger: OfferTrigger = when (offerType) {
        OfferType.PAYMENT_METHOD_DISCOUNT -> OfferTrigger.PaymentMethod(
            paymentCondition = t.paymentCondition ?: return null,
            minOrderTotalFils = t.minOrderTotal,
            minItemCount = t.minItemCount,
            maxItemCount = t.maxItemCount,
        )
        OfferType.ITEM_QTY_REWARD -> OfferTrigger.ItemSet(
            itemNumbers = t.itemNumbers ?: emptyList(),
            paymentCondition = t.paymentCondition,
        )
    }

    val reward: OfferReward = when (r.kind) {
        "LINE_PERCENT_DISCOUNT" -> OfferReward.LinePercent(
            basePercent = r.basePercent ?: 0.0,
            dynamic = r.mode == "DYNAMIC",
            multiplier = r.multiplier,
            itemsPerStep = r.itemsPerStep,
            maxPercent = r.maxPercent,
        )
        "LINE_AMOUNT_DISCOUNT" -> OfferReward.LineAmount(
            baseAmountFils = r.baseAmountFils ?: 0.0,
            dynamic = r.mode == "DYNAMIC",
            multiplier = r.multiplier,
            itemsPerStep = r.itemsPerStep,
            maxAmountFils = r.maxAmountFils,
            maxPercentOfPrice = r.maxPercentOfPrice,
        )
        "TABLE_AMOUNT_DISCOUNT" -> OfferReward.TableAmount(
            entries = (r.entries ?: return null).map {
                TableEntry(
                    itemNumber = it.itemNumber,
                    amountFils = it.amountFils,
                    maxPercentOfPrice = it.maxPercentOfPrice,
                )
            },
        )
        "TABLE_PERCENT_DISCOUNT" -> OfferReward.TablePercent(
            entries = (r.entries ?: return null).map {
                TableEntry(itemNumber = it.itemNumber, percent = it.percent)
            },
        )
        "GIFT" -> OfferReward.Gift(
            giftItems = r.giftItems ?: emptyList(),
            itemsPerGift = r.itemsPerGift ?: return null,
            giftsPerStep = r.giftsPerStep ?: 1,
            maxFreeQty = r.maxFreeQty,
        )
        "ITEM_PERCENT_DISCOUNT" -> OfferReward.ItemPercent(
            minQty = r.minQty ?: 0,
            basePercent = r.basePercent ?: 0.0,
            dynamic = r.mode == "DYNAMIC",
            multiplier = r.multiplier,
            itemsPerStep = r.itemsPerStep,
            maxPercent = r.maxPercent,
        )
        "ITEM_AMOUNT_DISCOUNT" -> OfferReward.ItemAmount(
            minQty = r.minQty ?: 0,
            baseAmountFils = r.baseAmountFils ?: 0.0,
            dynamic = r.mode == "DYNAMIC",
            multiplier = r.multiplier,
            itemsPerStep = r.itemsPerStep,
            maxAmountFils = r.maxAmountFils,
            maxPercentOfPrice = r.maxPercentOfPrice,
        )
        else -> return null
    }

    return OfferDefinition(
        id = id,
        name = name,
        type = offerType,
        trigger = trigger,
        reward = reward,
        eligibility = OfferEligibilityRule(
            customerScope = e?.customerScope ?: "ALL",
            segments = e?.segments,
            customerNumbers = e?.customerNumbers,
            regionIds = e?.regionIds,
            repIds = e?.repIds,
            storeNumbers = e?.storeNumbers,
        ),
        validFromMs = validFrom.toEpochMsOrNull(),
        validToMs = validTo.toEpochMsOrNull(),
        daysOfWeek = daysOfWeekCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() },
        timeFrom = timeFrom,
        timeTo = timeTo,
        totalRedemptionLimit = totalRedemptionLimit,
        perCustomerLimit = perCustomerLimit,
        priority = priority,
        redemptionCount = redemptionCount,
        createdAtMs = createdAt.toEpochMsOrNull() ?: 0L,
    )
}

private fun String?.toEpochMsOrNull(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

@Serializable
private data class TriggerJson(
    val paymentCondition: String? = null,
    val minOrderTotal: Long? = null,
    val minItemCount: Int? = null,
    val maxItemCount: Int? = null,
    val itemNumbers: List<String>? = null,
)

@Serializable
private data class RewardJson(
    val kind: String? = null,
    val basePercent: Double? = null,
    val mode: String? = null,
    val multiplier: Double? = null,
    val itemsPerStep: Int? = null,
    val maxPercent: Double? = null,
    val giftItems: List<String>? = null,
    val itemsPerGift: Int? = null,
    val giftsPerStep: Int? = null,
    val maxFreeQty: Int? = null,
    val minQty: Int? = null,
    val baseAmountFils: Double? = null,
    val maxAmountFils: Double? = null,
    val maxPercentOfPrice: Double? = null,
    val entries: List<TableEntryJson>? = null,
)

@Serializable
private data class TableEntryJson(
    val itemNumber: String,
    val amountFils: Double? = null,
    val percent: Double? = null,
    val maxPercentOfPrice: Double? = null,
)

@Serializable
private data class EligibilityJson(
    val customerScope: String? = null,
    val segments: List<String>? = null,
    val customerNumbers: List<String>? = null,
    val regionIds: List<String>? = null,
    val repIds: List<String>? = null,
    val storeNumbers: List<String>? = null,
)
