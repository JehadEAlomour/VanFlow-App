package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.MyRouteStopDto
import com.jehadalomour.flowvan.core.network.dto.RouteCycleDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postEmpty

/**
 * The signed-in salesman's own route (journey plan). Scoped server-side to the
 * authenticated rep, so it works for SALES / DRIVER accounts.
 */
class MyRouteApi(private val client: FlowVanApiClient) {

    /**
     * Outlets for today, ordered, with notes + to-dos.
     *
     * The server resolves "today" through the rep's own route cycle, so this is
     * correct whether they run a 7-day week or a 14-day rotation — the handset
     * never has to work out which day of the cycle it is.
     */
    suspend fun today(): List<MyRouteStopDto> = client.getData("my-route/today")

    /** The salesman's cycle: its length, start date, and today's position in it. */
    suspend fun cycle(): RouteCycleDto = client.getData("my-route/cycle")

    /**
     * Outlets for a given day of the rep's cycle (0..cycleDays-1). On the
     * default 7-day cycle that is 0=Sunday .. 6=Saturday.
     */
    suspend fun day(dayIndex: Int): List<MyRouteStopDto> =
        client.getData("my-route/day", mapOf("day" to dayIndex.toString()))

    /** Mark an outlet's to-do done for today. */
    suspend fun markTodoDone(customerId: String): MyRouteStopDto =
        client.postEmpty("my-route/$customerId/todo-done")
}
