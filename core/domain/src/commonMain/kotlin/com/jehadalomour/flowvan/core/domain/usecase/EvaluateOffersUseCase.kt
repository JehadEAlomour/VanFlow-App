package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.network.api.OfferApi
import com.jehadalomour.flowvan.core.network.dto.EvaluateLine
import com.jehadalomour.flowvan.core.network.dto.EvaluateRequest
import com.jehadalomour.flowvan.core.network.mapper.toOfferEvaluation

/**
 * Evaluates the offers engine for the current SALE cart and maps the result into the
 * domain [OfferEvaluation]. Stateless: free-item choices are sent simply as extra cart
 * lines (the server re-evaluates and treats them as free). The server is authoritative.
 */
class EvaluateOffersUseCase(
    private val offers: OfferApi,
) {
    suspend operator fun invoke(
        cart: List<CartLine>,
        customerNumber: String?,
        repId: String?,
        storeNumber: String?,
    ): Result<OfferEvaluation> = runCatching {
        if (cart.isEmpty()) return@runCatching OfferEvaluation.EMPTY

        val request = EvaluateRequest(
            customerNumber = customerNumber,
            repId = repId,
            storeNumber = storeNumber,
            at = null,                       // null → server "now"
            lines = cart.map { EvaluateLine(itemNumber = it.sku, qty = it.qty) },
        )
        offers.evaluate(request).toOfferEvaluation()
    }
}
