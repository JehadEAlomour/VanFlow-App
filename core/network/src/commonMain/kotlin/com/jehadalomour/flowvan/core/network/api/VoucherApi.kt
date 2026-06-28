package com.jehadalomour.flowvan.core.network.api

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.dto.CreateVoucherRequest
import com.jehadalomour.flowvan.core.network.dto.SyncVoucherResult
import com.jehadalomour.flowvan.core.network.dto.VoucherDetailDto
import com.jehadalomour.flowvan.core.network.dto.VoucherKindRequest
import com.jehadalomour.flowvan.core.network.dto.VoucherSummaryDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData
import io.ktor.http.HttpMethod

class VoucherApi(private val client: FlowVanApiClient) {
    private val log = Logger.withTag("VoucherApi")

    /**
     * Stage + post a voucher through the server inbox: the server assigns the
     * authoritative number (no client-chosen collisions) and dedupes by
     * `clientRef`, so retries are safe.
     */
    suspend fun create(body: CreateVoucherRequest): SyncVoucherResult =
        client.postData("sync/vouchers", body)

    /** A customer's SALE vouchers (server-numbered) — the source list for a return. */
    suspend fun customerSales(customerNumber: String): List<VoucherSummaryDto> =
        client.getData(
            "vouchers",
            mapOf("transKind" to "SALE", "customerNumber" to customerNumber),
        )

    /** Look up one SALE by its exact voucher number for a customer (manual return source). */
    suspend fun saleByNumber(
        voucherNumber: String,
        customerNumber: String,
    ): VoucherSummaryDto? =
        client.getData<List<VoucherSummaryDto>>(
            "vouchers",
            mapOf(
                "transKind" to "SALE",
                "voucherNumber" to voucherNumber,
                "customerNumber" to customerNumber,
            ),
        ).firstOrNull()

    /** A single voucher with its lines — to pre-fill a return from the sale. */
    suspend fun voucherDetail(id: String): VoucherDetailDto =
        client.getData("vouchers/$id")

    /** Idempotently create a transaction kind; ignores "already exists" conflicts. */
    suspend fun ensureKind(transKind: String, transName: String, sign: Int) {
        runCatching {
            client.execute(
                method = HttpMethod.Post,
                path = "vouchers/kinds",
                bodyJson = client.json.encodeToString(
                    VoucherKindRequest.serializer(),
                    VoucherKindRequest(transKind, transName, sign),
                ),
            )
        }.onFailure { log.d { "ensureKind($transKind): ${it.message}" } }
    }
}
