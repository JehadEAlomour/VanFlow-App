package com.jehadalomour.flowvan.core.model

/**
 * A parsed, engine-ready offer definition — the offline mirror of the backend's `Offer`
 * entity (see cash-van `offers.types.ts`). Money is **integer fils** (1 JOD = 1000 fils),
 * matching the server engine exactly so on-device evaluation is bit-identical.
 *
 * The two deployed offer types are [OfferType.PAYMENT_METHOD_DISCOUNT] and
 * [OfferType.ITEM_QTY_REWARD]. Offers whose `trigger`/`reward` JSON can't be parsed into
 * these shapes are dropped by the parser (forward-compat if a new type ships server-side).
 */
data class OfferDefinition(
    val id: String,
    val name: String,
    val type: OfferType,
    val trigger: OfferTrigger,
    val reward: OfferReward,
    val eligibility: OfferEligibilityRule,
    /** Schedule window (epoch ms), null = open-ended. */
    val validFromMs: Long?,
    val validToMs: Long?,
    /** Weekday numbers the offer runs on (0=Sun..6=Sat). Null = every day. */
    val daysOfWeek: List<Int>?,
    /** 'HH:mm' inclusive intra-day window. Null = all day. */
    val timeFrom: String?,
    val timeTo: String?,
    val totalRedemptionLimit: Int?,
    val perCustomerLimit: Int?,
    val priority: Int,
    val redemptionCount: Int,
    /** Server creation instant (epoch ms) — preserves the priority-tie ordering. */
    val createdAtMs: Long,
)

enum class OfferType { PAYMENT_METHOD_DISCOUNT, ITEM_QTY_REWARD }

sealed interface OfferTrigger {
    /** PAYMENT_METHOD_DISCOUNT: matches on payment condition + optional order thresholds. */
    data class PaymentMethod(
        /** "CASH" (any non-CREDIT payment) or "CREDIT". */
        val paymentCondition: String,
        /** Minimum order subtotal in fils, or null. */
        val minOrderTotalFils: Long?,
        /** Minimum total item count (sum of qty), or null. */
        val minItemCount: Int?,
    ) : OfferTrigger

    /** ITEM_QTY_REWARD: the offer's selected items; the trigger qty is their combined cart qty. */
    data class ItemSet(val itemNumbers: List<String>) : OfferTrigger
}

sealed interface OfferReward {
    /** A percentage discount applied to EVERY line of the order. */
    data class LinePercent(
        val basePercent: Double,
        val dynamic: Boolean,
        val multiplier: Double?,
        val itemsPerStep: Int?,
        val maxPercent: Double?,
    ) : OfferReward

    /**
     * A fixed amount (fils) off EVERY line of the order — the amount-off twin of [LinePercent],
     * for a PAYMENT_METHOD_DISCOUNT. Each line gets [baseAmountFils] off (flat per line,
     * independent of qty), clamped to the line gross by the evaluator.
     */
    data class LineAmount(
        val baseAmountFils: Double,
        val dynamic: Boolean,
        val multiplier: Double?,
        val itemsPerStep: Int?,
        val maxAmountFils: Double?,
    ) : OfferReward

    /** A gift the rep picks from [giftItems]; count derived from the trigger qty. */
    data class Gift(
        val giftItems: List<String>,
        val itemsPerGift: Int,
        val giftsPerStep: Int,
        val maxFreeQty: Int?,
    ) : OfferReward

    /** A percentage discount on the SELECTED items' lines once their combined qty ≥ [minQty]. */
    data class ItemPercent(
        val minQty: Int,
        val basePercent: Double,
        val dynamic: Boolean,
        val multiplier: Double?,
        val itemsPerStep: Int?,
        val maxPercent: Double?,
    ) : OfferReward

    /**
     * A fixed amount (fils) off EACH UNIT of the SELECTED items once their combined qty ≥ [minQty]
     * — the amount-off twin of [ItemPercent]. Per-line discount = amount × line qty, clamped to
     * the line gross by the evaluator.
     */
    data class ItemAmount(
        val minQty: Int,
        val baseAmountFils: Double,
        val dynamic: Boolean,
        val multiplier: Double?,
        val itemsPerStep: Int?,
        val maxAmountFils: Double?,
    ) : OfferReward
}

data class OfferEligibilityRule(
    /** ALL | SEGMENT | SPECIFIC | NEW_ONLY. */
    val customerScope: String,
    val segments: List<String>?,
    val customerNumbers: List<String>?,
    val regionIds: List<String>?,
    val repIds: List<String>?,
    val storeNumbers: List<String>?,
)
