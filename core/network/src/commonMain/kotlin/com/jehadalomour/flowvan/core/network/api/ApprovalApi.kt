package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.ApprovalRequestDto
import com.jehadalomour.flowvan.core.network.dto.CreateApprovalRequest
import com.jehadalomour.flowvan.core.network.dto.CreateVoucherRequest
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

/**
 * F10 — manager approval requests. When the backend answers a voucher POST
 * with 403 APPROVAL_REQUIRED:<type>, the same payload is re-filed here and the
 * manager decides from the dashboard; on approve the backend creates the
 * voucher itself.
 */
class ApprovalApi(private val client: FlowVanApiClient) {

    suspend fun create(
        type: String,
        payload: CreateVoucherRequest,
        note: String? = null,
        customerNumber: String? = null,
    ): ApprovalRequestDto =
        client.postData(
            "approvals",
            CreateApprovalRequest(
                type = type,
                payload = payload,
                note = note,
                customerNumber = customerNumber,
            ),
        )

    /** The salesman's own requests, newest first (poll for decisions). */
    suspend fun mine(status: String? = null): List<ApprovalRequestDto> =
        client.getData("approvals/mine", mapOf("status" to status))
}
