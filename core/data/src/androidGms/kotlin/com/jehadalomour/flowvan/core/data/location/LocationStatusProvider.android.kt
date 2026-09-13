package com.jehadalomour.flowvan.core.data.location

import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat

actual class LocationStatusProvider(private val context: Context) {
    actual fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }
}
