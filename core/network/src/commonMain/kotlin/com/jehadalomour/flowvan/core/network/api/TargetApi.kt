package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.TargetDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData

/** The signed-in salesman's own targets (read-only). */
class TargetApi(private val client: FlowVanApiClient) {

    /**
     * Target history for the last [months] months, most-recent first — each with the
     * target, actual sales and progress. Index 0 is the current month. Backed by
     * `GET /targets/me/history`.
     */
    suspend fun history(months: Int = 6): List<TargetDto> =
        client.getData("targets/me/history", mapOf("months" to months.toString()))
}
