package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.TobaccoTaxProfileDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData

/** GET /tobacco-tax-profiles — active tobacco tax profiles for the offline cache. */
class TobaccoTaxProfileApi(private val client: FlowVanApiClient) {
    suspend fun list(): List<TobaccoTaxProfileDto> = client.getData("tobacco-tax-profiles")
}
