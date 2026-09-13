package com.jehadalomour.flowvan.core.data.location

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tracking, in a build that does not track. Nothing is started, nothing is
 * emitted, and no trail is ever recorded.
 *
 * [isTracking] stays false so callers that ask before starting do not believe a
 * session is running; the flow is a real one that simply never emits, so
 * LocationTrackingCoordinator's collector suspends forever instead of the
 * coordinator needing to know which build it is in.
 */
class NoLocationTracker : LocationTracker {
    override val isTracking: Boolean = false
    override val locationUpdates: SharedFlow<LocationSample> =
        MutableSharedFlow<LocationSample>().asSharedFlow()

    override fun startTracking(shiftId: String, userId: String) = Unit
    override fun stopTracking() = Unit
}
