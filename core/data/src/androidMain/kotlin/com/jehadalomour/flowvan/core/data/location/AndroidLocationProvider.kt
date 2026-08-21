package com.jehadalomour.flowvan.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Best-effort device location, resilient to two things that made the first
 * version return null on real phones:
 *
 *  1. Play Services absent. The fused client throws on GMS-less devices (many
 *     Huawei/AOSP handsets), so we fall back to the platform LocationManager,
 *     which every Android device has.
 *  2. No cached fix. `lastLocation` — fused OR platform — is only whatever some
 *     app requested recently; on a cold device it is null even with GPS on and
 *     permission granted. When it is, we actively request ONE fresh fix rather
 *     than reporting failure.
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun lastLocation(): LatLng? {
        if (!hasPermission()) return null

        // 1. Fused cached fix — fastest when Play Services is present.
        fused()?.let { return it }

        // 2. Platform cached fix — works without Play Services.
        cachedFromManager()?.let { return it }

        // 3. Nothing cached: ask for a single fresh fix, bounded so the screen
        //    never hangs waiting on a device that will not answer.
        return withTimeoutOrNull(10_000) { singleUpdate() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fused(): LatLng? = runCatching {
        LocationServices.getFusedLocationProviderClient(context)
            .lastLocation.await()?.toLatLng()
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun cachedFromManager(): LatLng? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        // Most-accurate provider first; PASSIVE last since it is only whatever
        // another app happened to fetch.
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toLatLng()
    }

    @SuppressLint("MissingPermission")
    private suspend fun singleUpdate(): LatLng? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        return suspendCancellableCoroutine { cont: CancellableContinuation<LatLng?> ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location.toLatLng())
                }

                // Empty overrides kept for older API levels that still declare them abstract.
                override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
            runCatching {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.onFailure { if (cont.isActive) cont.resume(null) }
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        }
    }

    private fun Location.toLatLng() = LatLng(latitude, longitude)

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        // Coarse is enough for a 2 km prospecting search — do not demand fine.
        return fine || coarse
    }
}
