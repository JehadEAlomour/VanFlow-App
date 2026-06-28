package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceRequest
import com.jehadalomour.flowvan.core.network.dto.InvoiceDto
import com.jehadalomour.flowvan.core.network.dto.ReturnableLineDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import com.jehadalomour.flowvan.core.network.http.postEmpty

class InvoiceApi(private val client: FlowVanApiClient) {

    suspend fun list(
        repId: String? = null,
        customerId: String? = null,
        status: String? = null,
        from: String? = null,
        to: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): OffsetPage<InvoiceDto> = client.getData(
        path = "invoices",
        query = mapOf(
            "repId" to repId,
            "customerId" to customerId,
            "status" to status,
            "from" to from,
            "to" to to,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
        ),
    )

    suspend fun getById(id: String): InvoiceDto = client.getData("invoices/$id")

    suspend fun returnable(id: String): List<ReturnableLineDto> = client.getData("invoices/$id/returnable")

    suspend fun create(body: CreateInvoiceRequest): InvoiceDto = client.postData("invoices", body)

    suspend fun confirm(id: String): InvoiceDto = client.postEmpty("invoices/$id/confirm")
}
