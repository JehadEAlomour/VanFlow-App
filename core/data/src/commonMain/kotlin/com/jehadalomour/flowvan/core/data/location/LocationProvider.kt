package com.jehadalomour.flowvan.core.data.location

data class LatLng(val lat: Double, val lng: Double)

interface LocationProvider {
    /**
     * Returns the device's last known location, or null if unavailable
     * (no permission, no fix, hardware unavailable). Never throws.
     */
    suspend fun lastLocation(): LatLng?

    /**
     * Has the device GRANTED location to this app?
     *
     * Deliberately separate from [lastLocation] returning null, which conflates
     * three different situations: permission refused, permission granted but no
     * fix yet, and hardware unavailable. A rep who has allowed location and is
     * simply indoors must not be treated as one who switched it off — so the
     * location REQUIREMENT is decided on this, and never on a missing fix.
     */
    fun hasPermission(): Boolean
}