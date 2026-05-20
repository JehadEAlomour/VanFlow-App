package com.jehadalomour.flowvan.shared.data.location

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
    private val _updates = MutableSharedFlow<LatLng>(extraBufferCapacity = 128)

    override val locationUpdates: SharedFlow<LatLng> = _updates.asSharedFlow()
    override var isTracking: Boolean = false
        private set

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _updates.tryEmit(LatLng(loc.latitude, loc.longitude))
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking(shiftId: String, userId: String) {
        if (isTracking) return
        if (!hasFinePermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(10f)
            .setMaxUpdateDelayMillis(10_000L)
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
}
