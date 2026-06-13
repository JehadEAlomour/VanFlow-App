package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CreateCustomerRequest
import com.jehadalomour.flowvan.core.network.dto.CustomerDto
import com.jehadalomour.flowvan.core.network.dto.LogVisitRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import io.ktor.http.HttpMethod

class CustomerApi(private val client: FlowVanApiClient) {

    suspend fun list(
        q: String? = null,
        repId: String? = null,
        regionId: String? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): OffsetPage<CustomerDto> = client.getData(
        path = "customers",
        query = mapOf(
            "q" to q,
            "repId" to repId,
            "regionId" to regionId,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
        ),
    )

    suspend fun getById(id: String): CustomerDto = client.getData("customers/$id")

    suspend fun create(body: CreateCustomerRequest): CustomerDto = client.postData("customers", body)

    suspend fun logVisit(customerId: String, body: LogVisitRequest) {
        client.execute(
            method = HttpMethod.Post,
            path = "customers/$customerId/visits",
            bodyJson = client.json.encodeToString(LogVisitRequest.serializer(), body),
        )
    }
}
