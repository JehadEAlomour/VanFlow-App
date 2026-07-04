package com.jehadalomour.flowvan.core.data.heartbeat

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.location.LocationStatusProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.dto.HeartbeatRequest
import com.jehadalomour.flowvan.core.network.http.ApiConfig

/**
 * Sends a lightweight liveness heartbeat to the backend so the server can detect
 * when this rep drops off (internet lost / app closed) or disables GPS. Best-effort:
 * never throws — offline is the normal failure mode and simply means the server will
 * notice the silence on its own.
 */
class HeartbeatReporter(
    private val repApi: RepApi,
    private val session: SessionStore,
    private val locationStatus: LocationStatusProvider,
    private val apiConfig: ApiConfig,
) {
    private val log = Logger.withTag("HeartbeatReporter")

    /** @return true when the heartbeat was delivered. */
    suspend fun send(appState: String = "active"): Boolean {
        if (!apiConfig.isEnabled) return false
        val repId = session.currentRepId
        if (repId.isNullOrBlank()) return false
        if (session.currentToken.isNullOrBlank()) return false
        return try {
            repApi.postHeartbeat(
                repId,
                HeartbeatRequest(gpsEnabled = locationStatus.isGpsEnabled(), appState = appState),
            )
            true
        } catch (e: Exception) {
            log.d("heartbeat skipped: ${e.message}")
            false
        }
    }
}
