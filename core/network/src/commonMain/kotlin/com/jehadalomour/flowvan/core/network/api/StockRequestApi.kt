package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CreateStockRequestBody
import com.jehadalomour.flowvan.core.network.dto.MainStoreStockDto
import com.jehadalomour.flowvan.core.network.dto.StockRequestDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import com.jehadalomour.flowvan.core.network.http.postEmpty

/**
 * Asking the warehouse to load stock onto this van.
 *
 * The destination is always the caller's own van store — the server resolves it
 * from the rep, so there is nothing here to get wrong or to spoof. The source
 * warehouse is the manager's choice at approval time.
 */
class StockRequestApi(private val client: FlowVanApiClient) {

    suspend fun create(body: CreateStockRequestBody): StockRequestDto =
        client.postData("stock-requests", body)

    /** This rep's own requests, newest first. Polled while waiting on a decision. */
    suspend fun mine(): List<StockRequestDto> =
        client.getData("stock-requests/mine")

    /** Main depot stock per pool, shown as availability and enforced on create. */
    suspend fun mainStoreStock(): MainStoreStockDto =
        client.getData("stock-requests/main-store-stock")

    suspend fun cancel(id: String): StockRequestDto =
        client.postEmpty("stock-requests/$id/cancel")

    /**
     * Confirm the goods are physically on the van.
     *
     * This is what moves the stock: the server raises the transfer voucher here,
     * not at approval. Until it is called the van's stock is unchanged, however
     * long ago the office approved.
     */
    suspend fun receive(id: String): StockRequestDto =
        client.postEmpty("stock-requests/$id/receive")
}
