package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.network.api.OfferApi
import com.jehadalomour.flowvan.core.network.dto.EvaluateLine
import com.jehadalomour.flowvan.core.network.dto.EvaluateRequest
import com.jehadalomour.flowvan.core.network.mapper.toOfferEvaluation
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long to wait for the server's offer preview before falling back to the on-device cache.
 * Kept short so a bad/dead network (or a wrong "online" reading, e.g. the iOS connectivity stub)
 * doesn't stall the sale — offers should feel instant whether connected or not.
 */
private const val OFFER_EVAL_TIMEOUT_MS = 2_000L

/**
 * Evaluates offers for the current SALE cart. Online, the server is authoritative
 * (POST /offers/evaluate). Offline — or when the server call fails — it falls back to the
 * on-device [EvaluateOffersOfflineUseCase] over the cached offers, so the rep still sees
 * discounts/gifts (tagged [com.jehadalomour.flowvan.core.model.OfferSource.LOCAL]).
 *
 * The rep's GIFT picks ([chosenFreeItems], item numbers from an ITEM_QTY_REWARD gift pool)
 * are sent so free lines come back. The server re-validates and is the final arbiter on upload.
 */
class EvaluateOffersUseCase(
    private val offers: OfferApi,
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
    ): Result<OfferEvaluation> = runCatching {
        if (cart.isEmpty()) return@runCatching OfferEvaluation.EMPTY

        suspend fun local() =
            offline(cart, customerId, repId, storeNumber, paymentMethod, chosenFreeItems)

        if (connectivity.isOnline()) {
            val request = EvaluateRequest(
                customerNumber = customerNumber,
                repId = repId,
                storeNumber = storeNumber,
                paymentMethod = paymentMethod,   // CASH/CREDIT condition for payment-method offers
                at = null,                       // null → server "now"
                lines = cart.map { EvaluateLine(itemNumber = it.sku, qty = it.qty) },
                chosenFreeItems = chosenFreeItems,
            )
            // Try the authoritative server preview, but only briefly: on timeout OR any network
            // failure, fall back to the on-device cache so offers still show right away.
            val server = runCatching {
                withTimeoutOrNull(OFFER_EVAL_TIMEOUT_MS) {
                    offers.evaluate(request).toOfferEvaluation()
                }
            }.getOrNull()
            server ?: local()
        } else {
            local()
        }
    }
}
