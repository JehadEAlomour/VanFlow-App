package com.jehadalomour.flowvan.core.data.tracking

import com.jehadalomour.flowvan.core.data.location.LatLng
import kotlin.math.*

class StopDetector {
    private data class Anchor(val latLng: LatLng, val timeMs: Long)

    private var anchor: Anchor? = null
    private var stopFired = false

    /** Returns true the first time this point qualifies as a stop (within 50m for ≥3 min). */
    fun process(latLng: LatLng, timeMs: Long): Boolean {
        val current = anchor
        if (current == null) {
            anchor = Anchor(latLng, timeMs)
            return false
        }
        val distM = haversineMeters(current.latLng, latLng)
        if (distM > 50.0) {
            anchor = Anchor(latLng, timeMs)
            stopFired = false
            return false
        }
        val durationMs = timeMs - current.timeMs
        if (durationMs >= 3 * 60_000L && !stopFired) {
            stopFired = true
            return true
        }
        return false
    }

    fun reset() {
        anchor = null
        stopFired = false
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        val dLat = (b.lat - a.lat) * PI / 180.0
        val dLon = (b.lng - a.lng) * PI / 180.0
        val lat1 = a.lat * PI / 180.0
        val lat2 = b.lat * PI / 180.0
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(h))
    }
}
