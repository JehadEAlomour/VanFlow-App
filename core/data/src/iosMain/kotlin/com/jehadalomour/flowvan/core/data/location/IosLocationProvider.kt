package com.jehadalomour.flowvan.core.data.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {

    private val manager = CLLocationManager()

    override suspend fun lastLocation(): LatLng? {
        val status: CLAuthorizationStatus = CLLocationManager.authorizationStatus()
        if (status != kCLAuthorizationStatusAuthorizedWhenInUse &&
            status != kCLAuthorizationStatusAuthorizedAlways
        ) {
            // Best-effort prompt; the OS will show it once.
            manager.requestWhenInUseAuthorization()
            return null
        }
        val loc = manager.location ?: return null
        return loc.coordinate.useContents { LatLng(latitude, longitude) }
    }

    /** Authorised while in use, or always — anything else counts as denied. */
    override fun hasPermission(): Boolean {
        val status: CLAuthorizationStatus = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    /** Whether Location Services is on for the device as a whole. */
    override fun isServiceEnabled(): Boolean = CLLocationManager.locationServicesEnabled()
}
