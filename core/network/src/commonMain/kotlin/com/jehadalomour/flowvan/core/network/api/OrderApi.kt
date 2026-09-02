package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.OrderStockRowDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData

/** The ORDER flow's item source: the MAIN STORE's live stock (from the ERP). */
class OrderApi(private val client: FlowVanApiClient) {

    /**
     * Main-store stock for the ORDER picker — item quantities from the central depot.
     * JWT-authed route (no companyNumber/salesmanCode headers): the app signs with the
     * bearer token, and the main store's stock does not depend on which rep asks.
     */
    suspend fun orderStock(): List<OrderStockRowDto> = client.getData("mobile-order/stock")
}
