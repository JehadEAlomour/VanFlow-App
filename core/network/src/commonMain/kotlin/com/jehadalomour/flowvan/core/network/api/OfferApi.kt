package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.EvaluateRequest
import com.jehadalomour.flowvan.core.network.dto.EvaluationResultDto
import com.jehadalomour.flowvan.core.network.dto.OfferDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

/**
 * Offers engine API. `evaluate` is the live-preview call made on every cart change
 * (server authoritative); `activeOffers` returns the plain array of currently-active
 * offers for the offline cache.
 */
class OfferApi(private val client: FlowVanApiClient) {

    /** POST /offers/evaluate — stateless preview of offers for the current cart. */
    suspend fun evaluate(body: EvaluateRequest): EvaluationResultDto =
        client.postData("offers/evaluate", body)

    /** GET /offers/active — active offers for offline caching. */
    suspend fun activeOffers(
        customerNumber: String?,
        storeNumber: String?,
    ): List<OfferDto> =
        client.getData(
            "offers/active",
            mapOf(
                "customerNumber" to customerNumber,
                "storeNumber" to storeNumber,
            ),
        )
}
