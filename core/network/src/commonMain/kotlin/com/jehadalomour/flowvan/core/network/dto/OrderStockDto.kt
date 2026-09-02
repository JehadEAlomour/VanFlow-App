package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * One row from `GET /mobile/order-stock` — an ORDER draws from the MAIN STORE (the
 * central depot, live from the ERP), not the van, so this carries the main-store
 * quantity per item pool. `itemQty` is an integer string (the endpoint truncates).
 */
@Serializable
data class OrderStockRowDto(
    val itemNumber: String,
    val stockUnitCode: String = "",
    val itemQty: String = "0",
    val storeNumber: String? = null,
)
