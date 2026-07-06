package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.PriceListDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData

/** Price lists API. `full` returns every active list + its item prices for the offline cache. */
class PriceListApi(private val client: FlowVanApiClient) {

    /** GET /price-lists/full — all active price lists with their item prices. */
    suspend fun full(): List<PriceListDto> = client.getData("price-lists/full")
}
