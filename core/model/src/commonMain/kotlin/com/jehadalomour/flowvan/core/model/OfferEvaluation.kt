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
    /**
     * The server's authoritative per-line result (the server-fed cart). When non-empty
     * the UI renders these lines (merging product name/avatar by itemNumber) instead of
     * computing line totals on-device.
     */
    val serverLines: List<ServerLine> = emptyList(),
    /** The server's authoritative totals — preferred over on-device calculation when online. */
    val totals: OfferTotals = OfferTotals.ZERO,
    /** Whether this evaluation came from the server or the on-device offline evaluator. */
    val source: OfferSource = OfferSource.SERVER,
) {
    companion object {
        val EMPTY = OfferEvaluation()
    }
}

/**
 * Where an [OfferEvaluation] was computed. LOCAL results are provisional — the server re-applies
 * on sync. LOCAL_CONTRACT means we evaluated on-device *on purpose* (even while online) because
 * the customer has a contracted price list the server's /offers/evaluate can't honor — it re-prices
 * from the base catalog. It carries the price-list prices, so the UI treats it like a live result
 * (no "offline" banner), not a fallback.
 */
enum class OfferSource { SERVER, LOCAL, LOCAL_CONTRACT }

/** A line of the server-fed cart: server-computed unit price, discount, and net (JOD). */
data class ServerLine(
    val itemNumber: String,
    val qty: Double,
    val unitPriceJod: Double,
    val lineDiscountJod: Double,
    val lineNetJod: Double,
    /** The offer(s) that discounted this line, each with its % and JOD share. */
    val offers: List<LineOffer> = emptyList(),
) {
    /** Pre-discount line total (JOD) = unit price × qty. */
    val grossJod: Double get() = unitPriceJod * qty
    /** Combined effective discount fraction (0–1) across all offers on this line. */
    val discountFraction: Double get() = if (grossJod > 0) (lineDiscountJod / grossJod).coerceIn(0.0, 1.0) else 0.0
}

/** One offer's contribution to a single cart line (display label). */
data class LineOffer(
    val offerId: String,
    val name: String,
    val pct: Double,
    val discountJod: Double,
)

/** The server's invoice totals (JOD). Display-only; the server is authoritative on upload. */
data class OfferTotals(
    val subtotalJod: Double = 0.0,
    val lineDiscountJod: Double = 0.0,
    val invoiceDiscountJod: Double = 0.0,
    val totalDiscountJod: Double = 0.0,
    val taxJod: Double = 0.0,
    val grandTotalJod: Double = 0.0,
) {
    companion object {
        val ZERO = OfferTotals()
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
