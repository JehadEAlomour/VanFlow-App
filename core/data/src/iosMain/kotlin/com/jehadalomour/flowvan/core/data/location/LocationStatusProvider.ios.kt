package com.jehadalomour.flowvan.core.data.location

import platform.CoreLocation.CLLocationManager

actual class LocationStatusProvider {
    // Called off the main thread (from the sync loop), so the CoreLocation
    // main-thread advisory doesn't apply.
    actual fun isGpsEnabled(): Boolean = CLLocationManager.locationServicesEnabled()
}
