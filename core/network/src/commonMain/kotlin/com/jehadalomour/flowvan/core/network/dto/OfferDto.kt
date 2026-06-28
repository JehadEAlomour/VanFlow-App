package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Offers engine network contract. Money is **integer fils** (1 JOD = 1000 fils);
 * convert to JOD only at the mapper boundary. All fields are default-initialized so
 * `ignoreUnknownKeys = true` plus missing fields decode safely.
 *
 * See `cash-van-backend/docs/features/OFFERS-APP.md` for the UX spec. This is the
 * authoritative contract — the server re-evaluates on upload and is the final arbiter.
 */

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
data class EvaluateRequest(
    val customerNumber: String? = null,
    val repId: String? = null,
    val storeNumber: String? = null,
    // Order payment method — drives PAYMENT_METHOD_DISCOUNT. "CASH"|"CHEQUE"|"TRANSFER"|"CREDIT".
    val paymentMethod: String? = null,
    val at: String? = null,                 // ISO-8601 instant; null → server "now"
    val lines: List<EvaluateLine> = emptyList(),
    // Gifts the rep picked for ITEM_QTY_REWARD (GIFT) offers — item numbers chosen from
    // the offer's gift pool. When present, the server returns the chosen items as freeLines.
    val chosenFreeItems: List<String> = emptyList(),
)

@Serializable
data class EvaluateLine(
    val itemNumber: String = "",
    val qty: Double = 0.0,                   // a NUMBER, e.g. 6
)

// ── Evaluation result ─────────────────────────────────────────────────────────

@Serializable
data class EvaluationResultDto(
    val lines: List<EvalLineDto> = emptyList(),
    val freeLines: List<FreeLineDto> = emptyList(),
    val invoiceDiscountFils: Long = 0,
    val appliedOffers: List<AppliedOfferDto> = emptyList(),
    val totals: TotalsDto = TotalsDto(),
)

@Serializable
data class EvalLineDto(
    val itemNumber: String = "",
    val qty: Double = 0.0,
    val unitPriceFils: Long = 0,
    val lineDiscountFils: Long = 0,
    val lineNetFils: Long = 0,
    // The offer(s) that discounted this line, each with its % and fils share.
    val offers: List<LineOfferDto> = emptyList(),
)

@Serializable
data class LineOfferDto(
    val offerId: String = "",
    val name: String = "",
    val pct: Double = 0.0,
    val discountFils: Long = 0,
)

@Serializable
data class FreeLineDto(
    val itemNumber: String = "",
    val qty: Double = 0.0,
    val unitPriceFils: Long = 0,
    val offerId: String = "",
)

@Serializable
data class AppliedOfferDto(
    val offerId: String = "",
    val name: String = "",
    val type: String = "",
    val summary: String = "",
    val discountFils: Long = 0,
    val freeItems: List<FreeItemDto> = emptyList(),
    val freeItemChoice: FreeItemChoiceDto? = null,
)

@Serializable
data class FreeItemDto(
    val itemNumber: String = "",
    val qty: Int = 0,
)

@Serializable
data class FreeItemChoiceDto(
    val choices: List<String> = emptyList(),
    val qty: Int = 0,
)

@Serializable
data class TotalsDto(
    val subtotalFils: Long = 0,
    val lineDiscountFils: Long = 0,
    val invoiceDiscountFils: Long = 0,
    val totalDiscountFils: Long = 0,
    val taxFils: Long = 0,
    val grandTotalFils: Long = 0,
)

// ── Active offers (offline cache) ─────────────────────────────────────────────

@Serializable
data class OfferDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val type: String = "",
    val trigger: JsonObject? = null,
    val reward: JsonObject? = null,
    val eligibility: JsonObject? = null,
    val validFrom: String? = null,
    val validTo: String? = null,
    val priority: Int = 0,
    val stackable: Boolean = false,
    val isActive: Boolean = true,
    val redemptionCount: Int = 0,
    val status: String? = null,
)
