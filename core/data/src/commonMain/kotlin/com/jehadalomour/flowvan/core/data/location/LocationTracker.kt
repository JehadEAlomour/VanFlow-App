package com.jehadalomour.flowvan.core.data.location

import kotlinx.coroutines.flow.SharedFlow

/** One GPS fix as captured on-device — true capture time, not upload time. */
data class LocationSample(
    val latLng: LatLng,
    val accuracyM: Float?,
    val timeMs: Long,
)

interface LocationTracker {
    val isTracking: Boolean
    val locationUpdates: SharedFlow<LocationSample>
    fun startTracking(shiftId: String, userId: String)
    fun stopTracking()
}
