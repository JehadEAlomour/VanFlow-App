package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CollectionDto
import com.jehadalomour.flowvan.core.network.dto.CollectionSummaryDto
import com.jehadalomour.flowvan.core.network.dto.CreateCollectionRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import com.jehadalomour.flowvan.core.network.http.postEmpty

class CollectionApi(private val client: FlowVanApiClient) {

    suspend fun list(
        repId: String? = null,
        customerId: String? = null,
        method: String? = null,
        status: String? = null,
        from: String? = null,
        to: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): OffsetPage<CollectionDto> = client.getData(
        path = "collections",
        query = mapOf(
            "repId" to repId,
            "customerId" to customerId,
            "method" to method,
            "status" to status,
            "from" to from,
            "to" to to,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
        ),
    )

    suspend fun summary(date: String? = null): CollectionSummaryDto =
        client.getData("collections/summary", mapOf("date" to date))

    suspend fun create(body: CreateCollectionRequest): CollectionDto = client.postData("collections", body)

    suspend fun confirm(id: String): CollectionDto = client.postEmpty("collections/$id/confirm")
}
