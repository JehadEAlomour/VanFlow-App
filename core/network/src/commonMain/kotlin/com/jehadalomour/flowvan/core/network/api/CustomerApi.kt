package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CreateCustomerRequest
import com.jehadalomour.flowvan.core.network.dto.CustomerDto
import com.jehadalomour.flowvan.core.network.dto.ErpStatementDto
import com.jehadalomour.flowvan.core.network.dto.StagedPhotoDto
import com.jehadalomour.flowvan.core.network.http.ApiEnvelope
import com.jehadalomour.flowvan.core.network.dto.LogVisitRequest
import com.jehadalomour.flowvan.core.network.dto.SeedLocationRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What POST /customers produced. A salesman WITHOUT canCreateCustomerDirect gets
 * an approval request, not a customer — a different JSON shape entirely, and
 * decoding it as a CustomerDto throws on the missing id.
 */
sealed interface CreateCustomerOutcome {
    data class Created(val customer: CustomerDto) : CreateCustomerOutcome
    data class PendingApproval(val approvalId: String) : CreateCustomerOutcome
}

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

    /** POST /customers, tolerating both outcomes. See [CreateCustomerOutcome]. */
    suspend fun createOrRequest(body: CreateCustomerRequest): CreateCustomerOutcome {
        val text = client.execute(
            HttpMethod.Post,
            "customers",
            bodyJson = client.json.encodeToString(CreateCustomerRequest.serializer(), body),
        )
        val data = client.json.parseToJsonElement(text).jsonObject["data"]?.jsonObject
            ?: error("customers create returned no data")
        val pending = data["pendingApprovalId"]?.jsonPrimitive?.contentOrNull
        return if (pending != null) {
            CreateCustomerOutcome.PendingApproval(pending)
        } else {
            CreateCustomerOutcome.Created(
                client.json.decodeFromJsonElement(CustomerDto.serializer(), data),
            )
        }
    }

    /**
     * Upload the customer's document photo BEFORE creating them, and get the id
     * to pass as [CreateCustomerRequest.photoId].
     *
     * Two steps rather than one multipart create because a salesman creation may
     * sit in an approval queue for hours — the bytes are staged server-side and
     * only become a real attachment once the customer exists.
     */
    suspend fun uploadDocumentPhoto(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): StagedPhotoDto = client.json.decodeFromString<ApiEnvelope<StagedPhotoDto>>(
        client.executeMultipart("customers/photo", fileName, mimeType, bytes),
    ).data

    /**
     * Seed a customer's GPS location (seed-once server-side: only fills an empty
     * pin). Used to bootstrap a store that has no coordinates when a location-locked
     * rep opens it. Returns the customer (with the location if it took effect).
     */
    suspend fun seedLocation(customerId: String, lat: Double, lng: Double): CustomerDto =
        client.postData("customers/$customerId/location", SeedLocationRequest(lat, lng))

    /**
     * MOVE the customer's pin to these coordinates (overwrite) — the rep's "update
     * customer location" button. Unlike [seedLocation], this replaces an existing pin.
     */
    suspend fun updateLocation(customerId: String, lat: Double, lng: Double): CustomerDto =
        client.postData(
            "customers/$customerId/location",
            SeedLocationRequest(lat, lng, overwrite = true),
        )

    /**
     * The customer's account as the ERP keeps it, for a date window.
     *
     * The office invoices a shop in the ERP, not through a van, so those documents
     * exist in no cash-van voucher and the app's own ledger shows nothing for them.
     * This is the account the customer's balance is actually made of.
     *
     * Returns an unavailable envelope rather than failing when the customer has no
     * ERP code, ERP mode is off, or the ERP could not be reached — see
     * [ErpStatementDto.isUsable].
     */
    suspend fun erpStatement(
        customerId: String,
        from: String? = null,
        to: String? = null,
    ): ErpStatementDto {
        val query = listOfNotNull(
            from?.let { "from=$it" },
            to?.let { "to=$it" },
        ).joinToString("&")
        val suffix = if (query.isEmpty()) "" else "?$query"
        return client.getData("customers/$customerId/erp-statement$suffix")
    }

    suspend fun logVisit(customerId: String, body: LogVisitRequest) {
        client.execute(
            method = HttpMethod.Post,
            path = "customers/$customerId/visits",
            bodyJson = client.json.encodeToString(LogVisitRequest.serializer(), body),
        )
    }
}
