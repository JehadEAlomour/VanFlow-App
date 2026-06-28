package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.network.api.OfferApi
import com.jehadalomour.flowvan.core.network.dto.EvaluateLine
import com.jehadalomour.flowvan.core.network.dto.EvaluateRequest
import com.jehadalomour.flowvan.core.network.mapper.toOfferEvaluation

/**
 * Evaluates the offers engine for the current SALE cart and maps the result into the
 * domain [OfferEvaluation]. The rep's GIFT picks ([chosenFreeItems], item numbers from an
 * ITEM_QTY_REWARD gift pool) are sent so the server returns them as FREE lines. The server
 * is authoritative — it validates the picks against each offer's gift pool.
 */
class EvaluateOffersUseCase(
    private val offers: OfferApi,
) {
    suspend operator fun invoke(
        cart: List<CartLine>,
        customerNumber: String?,
        repId: String?,
        storeNumber: String?,
        paymentMethod: String?,
        chosenFreeItems: List<String> = emptyList(),
    ): Result<OfferEvaluation> = runCatching {
        if (cart.isEmpty()) return@runCatching OfferEvaluation.EMPTY

        val request = EvaluateRequest(
            customerNumber = customerNumber,
            repId = repId,
            storeNumber = storeNumber,
            paymentMethod = paymentMethod,   // CASH/CREDIT condition for payment-method offers
            at = null,                       // null → server "now"
            lines = cart.map { EvaluateLine(itemNumber = it.sku, qty = it.qty) },
            chosenFreeItems = chosenFreeItems,
        )
        offers.evaluate(request).toOfferEvaluation()
    }
}
