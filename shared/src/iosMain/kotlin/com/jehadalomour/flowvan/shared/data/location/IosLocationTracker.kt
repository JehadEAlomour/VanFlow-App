package com.jehadalomour.flowvan.shared.data.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class IosLocationTracker : LocationTracker {

    private val provider = IosLocationProvider()
    private val _updates = MutableSharedFlow<LatLng>(extraBufferCapacity = 128)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override val locationUpdates: SharedFlow<LatLng> = _updates.asSharedFlow()
    override var isTracking: Boolean = false
        private set

    override fun startTracking(shiftId: String, userId: String) {
        if (isTracking) return
        isTracking = true
        pollJob = scope.launch {
            while (isActive) {
                provider.lastLocation()?.let { _updates.emit(it) }
                delay(5_000L)
            }
        }
    }

    override fun stopTracking() {
        pollJob?.cancel()
        pollJob = null
        isTracking = false
    }
}
