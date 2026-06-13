package com.jehadalomour.flowvan.core.data.location

data class LatLng(val lat: Double, val lng: Double)

interface LocationProvider {
    /**
     * Returns the device's last known location, or null if unavailable
     * (no permission, no fix, hardware unavailable). Never throws.
     */
    suspend fun lastLocation(): LatLng?
}