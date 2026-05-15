package com.jehadalomour.flowvan.shared.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun lastLocation(): LatLng? {
        if (!hasFinePermission()) return null
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.await()?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }

    private fun hasFinePermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}
