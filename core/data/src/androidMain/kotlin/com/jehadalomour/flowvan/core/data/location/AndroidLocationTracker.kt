package com.jehadalomour.flowvan.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidLocationTracker(
    private val context: Context,
    private val onStartService: (Context) -> Unit,
    private val onStopService: (Context) -> Unit,
) : LocationTracker {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val _updates = MutableSharedFlow<LocationSample>(extraBufferCapacity = 128)

    override val locationUpdates: SharedFlow<LocationSample> = _updates.asSharedFlow()
    override var isTracking: Boolean = false
        private set

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                // Drop garbage fixes — they make trails zigzag and waste upload quota.
                if (loc.hasAccuracy() && loc.accuracy > MAX_ACCURACY_M) continue
                _updates.tryEmit(
                    LocationSample(
                        latLng = LatLng(loc.latitude, loc.longitude),
                        accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                        timeMs = loc.time,
                    ),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking(shiftId: String, userId: String) {
        if (isTracking) return
        if (!hasFinePermission()) return
        // All-day cadence: a fix every ~30s while moving, nothing when parked
        // (20m displacement filter), batched up to 2 min so the radio can sleep.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateDistanceMeters(20f)
            .setMaxUpdateDelayMillis(120_000L)
            .build()
        fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
        onStartService(context)
        isTracking = true
    }

    override fun stopTracking() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(callback)
        onStopService(context)
        isTracking = false
    }

    private fun hasFinePermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val MAX_ACCURACY_M = 100f
    }
}
