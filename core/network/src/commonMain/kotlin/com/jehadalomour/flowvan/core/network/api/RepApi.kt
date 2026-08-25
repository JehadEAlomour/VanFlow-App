package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.HeartbeatRequest
import com.jehadalomour.flowvan.core.network.dto.HeartbeatResultDto
import com.jehadalomour.flowvan.core.network.dto.LocationBulkRequest
import com.jehadalomour.flowvan.core.network.dto.LocationBulkResultDto
import com.jehadalomour.flowvan.core.network.dto.LocationPingDto
import com.jehadalomour.flowvan.core.network.dto.LocationPingRequest
import com.jehadalomour.flowvan.core.network.dto.ErpBalanceDto
import com.jehadalomour.flowvan.core.network.dto.RepKpiDto
import com.jehadalomour.flowvan.core.network.dto.VanStockItemDto
import com.jehadalomour.flowvan.core.network.dto.RepMaterialsDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

class RepApi(private val client: FlowVanApiClient) {

    suspend fun kpis(repId: String): RepKpiDto = client.getData("reps/$repId/kpis")

    /** The signed-in rep's live "cash with salesman" GL balance from the ERP. */
    suspend fun myErpBalance(): ErpBalanceDto = client.getData("reps/me/erp-balance")

    /** Read-only: per-rep van stock (server derives it from posted voucher transactions). */
    suspend fun vanStock(repId: String): List<VanStockItemDto> = client.getData("reps/$repId/van-stock")

    /** The signed-in rep's materials grouped by warehouse (read-only). */
    suspend fun myMaterialsByWarehouse(): RepMaterialsDto =
        client.getData("reps/me/materials-by-warehouse")

    suspend fun postLocation(repId: String, body: LocationPingRequest): LocationPingDto =
        client.postData("reps/$repId/location", body)

    suspend fun postLocationBulk(repId: String, points: List<LocationPingRequest>): LocationBulkResultDto =
        client.postData("reps/$repId/location/bulk", LocationBulkRequest(points))

    /** Liveness heartbeat: reports GPS-enabled + app state so the server can detect disconnections. */
    suspend fun postHeartbeat(repId: String, body: HeartbeatRequest): HeartbeatResultDto =
        client.postData("reps/$repId/heartbeat", body)
}
