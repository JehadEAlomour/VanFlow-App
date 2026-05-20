package com.jehadalomour.flowvan.shared.data.location

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
}
