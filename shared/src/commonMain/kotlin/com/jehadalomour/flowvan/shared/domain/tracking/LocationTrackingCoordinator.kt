package com.jehadalomour.flowvan.shared.domain.tracking

import com.jehadalomour.flowvan.shared.data.location.LocationTracker
import com.jehadalomour.flowvan.shared.data.repository.LocationRepository
import com.jehadalomour.flowvan.shared.data.tracking.StopDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LocationTrackingCoordinator(
    private val locationTracker: LocationTracker,
    private val locationRepository: LocationRepository,
    private val stopDetector: StopDetector,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    val isTracking: Boolean get() = locationTracker.isTracking

    @OptIn(ExperimentalTime::class)
    fun start(shiftId: String, userId: String) {
        if (locationTracker.isTracking) return
        locationTracker.startTracking(shiftId, userId)
        collectJob?.cancel()
        collectJob = scope.launch {
            locationTracker.locationUpdates.collect { latLng ->
                val nowMs = Clock.System.now().toEpochMilliseconds()
                locationRepository.savePoint(shiftId, userId, latLng, null)
                stopDetector.process(latLng, nowMs)
            }
        }
    }

    fun stop() {
        locationTracker.stopTracking()
        collectJob?.cancel()
        collectJob = null
        stopDetector.reset()
    }
}
