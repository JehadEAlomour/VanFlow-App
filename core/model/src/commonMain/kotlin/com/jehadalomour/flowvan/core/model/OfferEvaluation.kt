package com.jehadalomour.flowvan.core.model

/**
 * Domain result of an offers evaluation. Money is JOD (major units, Double) — the
 * network mapper converts fils → JOD at its boundary. Discounts here are **display
 * only**; the server re-evaluates and is authoritative on upload.
 */
data class OfferEvaluation(
    /** Per-line discount adjustments to overlay on matching cart lines (keyed by itemNumber/sku). */
    val adjustedLines: List<OfferLineAdj> = emptyList(),
    /** Free items the offers add to the cart as net-0 lines. */
    val freeLines: List<FreeLine> = emptyList(),
    /** Invoice-level offer discount (JOD). */
    val invoiceDiscountJod: Double = 0.0,
    /** Offers that were applied — drives the banner chips. */
    val appliedOffers: List<AppliedOffer> = emptyList(),
    /** Offers awaiting a free-item pick — drives the choose-free-item sheet. */
    val pendingChoices: List<OfferChoice> = emptyList(),
) {
    companion object {
        val EMPTY = OfferEvaluation()
    }
}

/** A discount the engine applied to a specific cart line (display overlay). */
data class OfferLineAdj(
    val itemNumber: String,
    val discountJod: Double,
)

/** A free item added by an offer: a normal cart line at its real price, netted to 0. */
data class FreeLine(
    val itemNumber: String,
    val qty: Double,
    val unitPriceJod: Double,
    val offerId: String,
)

/** An applied offer, for the banner. */
data class AppliedOffer(
    val offerId: String,
    val name: String,
    val summary: String,
    val type: String,
)

/** An offer that needs the rep to pick one free item from a list. */
data class OfferChoice(
    val offerId: String,
    val choices: List<String>,
    val qty: Int,
)
