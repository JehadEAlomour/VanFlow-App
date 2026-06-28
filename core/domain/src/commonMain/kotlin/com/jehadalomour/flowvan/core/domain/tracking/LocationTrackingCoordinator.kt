package com.jehadalomour.flowvan.core.domain.tracking

import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationTracker
import com.jehadalomour.flowvan.core.data.repository.LocationRepository
import com.jehadalomour.flowvan.core.data.tracking.StopDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationTrackingCoordinator(
    private val locationTracker: LocationTracker,
    private val locationRepository: LocationRepository,
    private val stopDetector: StopDetector,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    // Last point actually enqueued — used to drop stationary duplicates (F12 §4).
    private var lastEnqueued: LatLng? = null
    private var lastEnqueuedMs: Long = 0L

    val isTracking: Boolean get() = locationTracker.isTracking

    fun start(shiftId: String, userId: String) {
        if (locationTracker.isTracking) return
        locationTracker.startTracking(shiftId, userId)
        collectJob?.cancel()
        collectJob = scope.launch {
            locationTracker.locationUpdates.collect { sample ->
                // De-dupe / stationary skip: drop a fix < 10 m from the last enqueued one
                // *and* < 60 s newer — avoids hundreds of identical pings while parked.
                val prev = lastEnqueued
                if (prev != null &&
                    haversineMeters(prev, sample.latLng) < STATIONARY_RADIUS_M &&
                    sample.timeMs - lastEnqueuedMs < STATIONARY_WINDOW_MS
                ) {
                    return@collect
                }
                locationRepository.savePoint(
                    shiftId = shiftId,
                    userId = userId,
                    latLng = sample.latLng,
                    accuracy = sample.accuracyM,
                    recordedAtMs = sample.timeMs,
                )
                lastEnqueued = sample.latLng
                lastEnqueuedMs = sample.timeMs
                stopDetector.process(sample.latLng, sample.timeMs)
            }
        }
    }

    fun stop() {
        locationTracker.stopTracking()
        collectJob?.cancel()
        collectJob = null
        lastEnqueued = null
        lastEnqueuedMs = 0L
        stopDetector.reset()
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        val dLat = (b.lat - a.lat).toRadians()
        val dLng = (b.lng - a.lng).toRadians()
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * asin(sqrt(h))
    }

    private fun Double.toRadians(): Double = this * 0.017453292519943295 // PI / 180

    companion object {
        /** Shift id recorded on points captured outside an active shift (always-on tracking). */
        const val ALWAYS_ON_SHIFT_ID = "always-on"
        private const val STATIONARY_RADIUS_M = 10.0
        private const val STATIONARY_WINDOW_MS = 60_000L
    }
}
