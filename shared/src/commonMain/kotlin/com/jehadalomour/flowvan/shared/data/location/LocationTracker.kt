package com.jehadalomour.flowvan.shared.data.location

import kotlinx.coroutines.flow.SharedFlow

interface LocationTracker {
    val isTracking: Boolean
    val locationUpdates: SharedFlow<LatLng>
    fun startTracking(shiftId: String, userId: String)
    fun stopTracking()
}
