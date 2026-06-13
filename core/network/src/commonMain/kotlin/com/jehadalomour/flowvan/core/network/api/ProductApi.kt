package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.ProductDto
import com.jehadalomour.flowvan.core.network.dto.QuoteDto
import com.jehadalomour.flowvan.core.network.dto.QuoteRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

class ProductApi(private val client: FlowVanApiClient) {

    suspend fun list(
        q: String? = null,
        categoryId: String? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): OffsetPage<ProductDto> = client.getData(
        path = "products",
        query = mapOf(
            "q" to q,
            "categoryId" to categoryId,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
        ),
    )

    suspend fun getById(id: String): ProductDto = client.getData("products/$id")

    suspend fun quote(productId: String, qty: Double, customerId: String? = null): QuoteDto =
        client.postData("products/$productId/quote", QuoteRequest(qty, customerId))
}
