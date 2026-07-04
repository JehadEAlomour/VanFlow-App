package com.jehadalomour.flowvan.core.data.location

/**
 * Reports whether device location services (GPS) are currently enabled. Used by
 * the liveness heartbeat so the server can alert admins when a rep disables GPS.
 */
expect class LocationStatusProvider {
    fun isGpsEnabled(): Boolean
}
