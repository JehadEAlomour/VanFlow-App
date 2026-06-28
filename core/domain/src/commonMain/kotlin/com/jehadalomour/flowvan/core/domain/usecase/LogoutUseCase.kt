package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator

class LogoutUseCase(
    private val session: SessionStore,
    private val coordinator: LocationTrackingCoordinator,
) {
    operator fun invoke() {
        // Tracking is session-scoped: signed out ⇒ no trail, and the
        // foreground service (with its notification) goes away too.
        coordinator.stop()
        session.clear()
    }
}
