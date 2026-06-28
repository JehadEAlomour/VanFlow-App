package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.MyRouteStopDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postEmpty

/**
 * The signed-in salesman's own route (journey plan). Scoped server-side to the
 * authenticated rep, so it works for SALES / DRIVER accounts.
 */
class MyRouteApi(private val client: FlowVanApiClient) {

    /** Outlets for today, ordered, with notes + to-dos. */
    suspend fun today(): List<MyRouteStopDto> = client.getData("my-route/today")

    /** Outlets for a given weekday (0=Sunday .. 6=Saturday). */
    suspend fun day(weekday: Int): List<MyRouteStopDto> =
        client.getData("my-route/day", mapOf("weekday" to weekday.toString()))

    /** Mark an outlet's to-do done for today. */
    suspend fun markTodoDone(customerId: String): MyRouteStopDto =
        client.postEmpty("my-route/$customerId/todo-done")
}
