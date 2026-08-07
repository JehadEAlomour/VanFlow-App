package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.ConfirmReturnRequest
import com.jehadalomour.flowvan.core.network.dto.ReturnPlanDto
import com.jehadalomour.flowvan.core.network.dto.ReturnPreviewRequest
import com.jehadalomour.flowvan.core.network.dto.ReturnPreviewResponse
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.postData

/**
 * Returning goods without naming the sale they came from.
 *
 * See cash-van-dashboard/docs/RETURNS-without-a-sale-voucher.md. Two calls, not
 * one: an item can span several past sales and different items can come from
 * different sales, so how many vouchers a confirm creates is not knowable from
 * the request. The rep sees the match before it exists — they are standing in
 * front of the customer being asked "which invoice?".
 */
class ReturnByItemApi(private val client: FlowVanApiClient) {

    /** Match without creating anything. */
    suspend fun preview(body: ReturnPreviewRequest): ReturnPreviewResponse =
        client.postData("vouchers/returns/preview", body)

    /**
     * Create the return vouchers — one per source sale.
     *
     * Sends the REQUEST again, never the previewed plan: the server re-allocates
     * and re-checks each source line under a row lock, so a plan held on the
     * device while the rep talked to the customer cannot credit units someone
     * else has already returned.
     */
    suspend fun confirm(body: ConfirmReturnRequest): ReturnPlanDto.Created =
        client.postData("vouchers/returns/confirm", body)
}
