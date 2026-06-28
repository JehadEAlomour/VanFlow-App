package com.jehadalomour.flowvan.core.data.location

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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class IosLocationTracker : LocationTracker {

    private val provider = IosLocationProvider()
    private val _updates = MutableSharedFlow<LocationSample>(extraBufferCapacity = 128)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override val locationUpdates: SharedFlow<LocationSample> = _updates.asSharedFlow()
    override var isTracking: Boolean = false
        private set

    @OptIn(ExperimentalTime::class)
    override fun startTracking(shiftId: String, userId: String) {
        if (isTracking) return
        isTracking = true
        pollJob = scope.launch {
            while (isActive) {
                provider.lastLocation()?.let {
                    _updates.emit(
                        LocationSample(
                            latLng = it,
                            accuracyM = null,
                            timeMs = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                }
                delay(30_000L)
            }
        }
    }

    override fun stopTracking() {
        pollJob?.cancel()
        pollJob = null
        isTracking = false
    }
}
