package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.heartbeat.HeartbeatReporter
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.AuthApi
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sign out of the app — and nothing more.
 *
 * Tracking used to be session-scoped: signing out killed the trail and took the
 * foreground service with it, which made being tracked effectively opt-out. It
 * is not opt-out. A salesman is tracked for the working day whether or not the
 * app is open, so the tracker is deliberately left running here, on the device
 * tracking token, which the server honours until the office releases the
 * handset.
 *
 * What does end is the *interactive* session: the token that reads customers
 * and writes vouchers is cleared, so whoever picks the phone up next gets the
 * login screen and no data.
 */
class LogoutUseCase(
    private val session: SessionStore,
    private val heartbeat: HeartbeatReporter,
    private val authApi: AuthApi,
) {
    suspend operator fun invoke() {
        // Tell the server this is a deliberate day-close/sign-out BEFORE clearing
        // the token, so the offline watchdog won't raise a false "rep offline"
        // alert. Best-effort and time-boxed — never blocks logout.
        withTimeoutOrNull(2_000) { heartbeat.send(appState = "signed_out") }
        withTimeoutOrNull(2_000) {
            runCatching { authApi.logout(session.boundDeviceId) }
        }

        // No coordinator.stop() here, and none belongs here. `clear()` leaves
        // the tracking token and rep id behind, which is what the uploader
        // falls back to once the session token is gone.
        session.clear()
    }
}
