package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.heartbeat.HeartbeatReporter
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator
import kotlinx.coroutines.withTimeoutOrNull

class LogoutUseCase(
    private val session: SessionStore,
    private val coordinator: LocationTrackingCoordinator,
    private val heartbeat: HeartbeatReporter,
) {
    suspend operator fun invoke() {
        // Tell the server this is a deliberate day-close/sign-out BEFORE clearing
        // the token, so the offline watchdog won't raise a false "rep offline"
        // alert. Best-effort and time-boxed — never blocks logout.
        withTimeoutOrNull(2_000) { heartbeat.send(appState = "signed_out") }

        // Tracking is session-scoped: signed out ⇒ no trail, and the
        // foreground service (with its notification) goes away too.
        coordinator.stop()
        session.clear()
    }
}
