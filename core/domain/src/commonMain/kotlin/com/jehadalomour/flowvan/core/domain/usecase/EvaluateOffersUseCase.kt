package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.model.OfferSource

/**
 * Evaluates offers for the current SALE cart — ALWAYS on-device, via the on-device
 * [EvaluateOffersOfflineUseCase] over the cached offers ([com.jehadalomour.flowvan.core.data.repository.OfferRepository],
 * refreshed from GET /offers/active). Calculations never leave the device: there is deliberately
 * no POST /offers/evaluate round-trip, so offers feel instant and identical whether online or not.
 *
 * When online the result is tagged [OfferSource.LOCAL_CONTRACT] so the UI treats it as a LIVE
 * result (no "offline" banner); genuinely offline it stays [OfferSource.LOCAL] (banner shown).
 * The server still re-validates and remains the final arbiter when the RAW cart uploads on sync
 * (POST /sync/vouchers) — the app just no longer asks it to compute the preview.
 *
 * The rep's GIFT picks ([chosenFreeItems], item numbers from an ITEM_QTY_REWARD gift pool) are
 * passed through so free lines come back from the local evaluator.
 */
class EvaluateOffersUseCase(
    private val connectivity: ConnectivityObserver,
    private val offline: EvaluateOffersOfflineUseCase,
) {
    suspend operator fun invoke(
        cart: List<CartLine>,
        customerId: String?,
        customerNumber: String?,
        repId: String?,
        storeNumber: String?,
        paymentMethod: String?,
        chosenFreeItems: List<String> = emptyList(),
        hasContractPrices: Boolean = false,
    ): Result<OfferEvaluation> = runCatching {
        if (cart.isEmpty()) return@runCatching OfferEvaluation.EMPTY

        // Always evaluate on-device. Online → tag LOCAL_CONTRACT (a live, on-purpose on-device
        // result, no offline banner); offline → LOCAL (banner shown). See class KDoc.
        val result = offline(cart, customerId, repId, storeNumber, paymentMethod, chosenFreeItems)
        if (connectivity.isOnline()) result.copy(source = OfferSource.LOCAL_CONTRACT) else result
    }
}
