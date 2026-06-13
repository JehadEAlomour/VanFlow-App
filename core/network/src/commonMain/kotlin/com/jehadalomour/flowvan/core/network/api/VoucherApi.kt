package com.jehadalomour.flowvan.core.network.api

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.dto.CreateVoucherRequest
import com.jehadalomour.flowvan.core.network.dto.VoucherDto
import com.jehadalomour.flowvan.core.network.dto.VoucherKindRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.postData
import io.ktor.http.HttpMethod

class VoucherApi(private val client: FlowVanApiClient) {
    private val log = Logger.withTag("VoucherApi")

    suspend fun create(body: CreateVoucherRequest): VoucherDto = client.postData("vouchers", body)

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
