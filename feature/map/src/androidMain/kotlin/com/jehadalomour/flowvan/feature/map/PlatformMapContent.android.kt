package com.jehadalomour.flowvan.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@SuppressLint("MissingPermission")
@Composable
actual fun PlatformMapContent(
    userLat: Double?,
    userLng: Double?,
    customerLat: Double,
    customerLng: Double,
    customerName: String,
    modifier: Modifier,
    isNavigating: Boolean,
    onRouteInfo: (duration: String, distance: String) -> Unit,
    onStepsLoaded: (List<NavStep>) -> Unit,
    onLocationUpdate: (lat: Double, lng: Double) -> Unit,
) {
    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    val apiKey = remember {
        runCatching {
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        }.getOrDefault("")
    }
    val packageName = remember { context.packageName }
    val certSha1 = remember { getSignatureSha1(context) }

    val customerLatLng = LatLng(customerLat, customerLng)
    val userLatLng = if (userLat != null && userLng != null) LatLng(userLat, userLng) else null

    val cameraCenter = if (userLatLng != null) {
        LatLng((customerLat + userLat!!) / 2.0, (customerLng + userLng!!) / 2.0)
    } else customerLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraCenter, if (userLatLng != null) 12f else 14f)
    }

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Fetch route on first load
    LaunchedEffect(userLat, userLng, customerLat, customerLng) {
        if (userLatLng != null && apiKey.isNotEmpty()) {
            val result = fetchRoute(
                origin = userLatLng,
                destination = customerLatLng,
                apiKey = apiKey,
                packageName = packageName,
                certSha1 = certSha1,
            )
            routePoints = result.points
            if (result.duration.isNotEmpty()) onRouteInfo(result.duration, result.distance)
            if (result.steps.isNotEmpty()) onStepsLoaded(result.steps)
        }
    }

    // Navigation camera: follow user with tilt when navigating
    LaunchedEffect(userLat, userLng, isNavigating) {
        if (isNavigating && userLat != null && userLng != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(userLat, userLng))
                        .zoom(18f)
                        .tilt(45f)
                        .build(),
                ),
                durationMs = 800,
            )
        }
    }

    // Live location updates during navigation
    val currentOnLocationUpdate by rememberUpdatedState(onLocationUpdate)
    LaunchedEffect(isNavigating) {
        if (!isNavigating || !hasLocationPermission) return@LaunchedEffect
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                .setMinUpdateDistanceMeters(5f)
                .build()
            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(it) }
                }
            }
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
            awaitClose { fusedClient.removeLocationUpdates(cb) }
        }.catch { }.collect { loc ->
            currentOnLocationUpdate(loc.latitude, loc.longitude)
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = isNavigating,
            zoomControlsEnabled = !isNavigating,
            compassEnabled = true,
        ),
    ) {
        Marker(
            state = MarkerState(position = customerLatLng),
            title = customerName,
            snippet = "الوجهة",
        )
        when {
            routePoints.size >= 2 -> Polyline(
                points = routePoints,
                color = Color(0xFF4B8FF6),
                width = 12f,
            )
            userLatLng != null -> Polyline(
                points = listOf(userLatLng, customerLatLng),
                color = Color(0x664B8FF6),
                width = 6f,
                geodesic = true,
            )
        }
    }
}

private data class RouteData(
    val points: List<LatLng>,
    val duration: String,
    val distance: String,
    val steps: List<NavStep>,
)

private suspend fun fetchRoute(
    origin: LatLng,
    destination: LatLng,
    apiKey: String,
    packageName: String,
    certSha1: String,
): RouteData = withContext(Dispatchers.IO) {
    try {
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&mode=driving" +
            "&language=ar" +
            "&key=$apiKey"

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("X-Android-Package", packageName)
        connection.setRequestProperty("X-Android-Cert", certSha1)

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return@withContext RouteData(emptyList(), "", "", emptyList())
        }

        val json = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val jsonObj = JSONObject(json)
        if (jsonObj.getString("status") != "OK") return@withContext RouteData(emptyList(), "", "", emptyList())

        val route = jsonObj.getJSONArray("routes").getJSONObject(0)
        val leg = route.getJSONArray("legs").getJSONObject(0)
        val duration = leg.getJSONObject("duration").getString("text")
        val distance = leg.getJSONObject("distance").getString("text")
        val encoded = route.getJSONObject("overview_polyline").getString("points")

        val stepsArray = leg.getJSONArray("steps")
        val steps = (0 until stepsArray.length()).map { i ->
            val step = stepsArray.getJSONObject(i)
            val endLoc = step.getJSONObject("end_location")
            NavStep(
                instruction = step.getString("html_instructions").stripHtml(),
                distanceText = step.getJSONObject("distance").getString("text"),
                endLat = endLoc.getDouble("lat"),
                endLng = endLoc.getDouble("lng"),
            )
        }

        RouteData(decodePolyline(encoded), duration, distance, steps)
    } catch (e: Exception) {
        android.util.Log.e("MapRoute", "fetchRoute exception", e)
        RouteData(emptyList(), "", "", emptyList())
    }
}

private fun String.stripHtml(): String =
    replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

private fun decodePolyline(encoded: String): List<LatLng> {
    val result = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var value = 0
        do {
            b = encoded[index++].code - 63
            value = value or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (value and 1 != 0) (value shr 1).inv() else value shr 1
        shift = 0
        value = 0
        do {
            b = encoded[index++].code - 63
            value = value or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if (value and 1 != 0) (value shr 1).inv() else value shr 1
        result.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return result
}

@SuppressLint("PackageManagerGetSignatures")
private fun getSignatureSha1(context: android.content.Context): String {
    return try {
        val cert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.firstOrNull()?.toByteArray()
        } ?: return ""
        MessageDigest.getInstance("SHA-1").digest(cert)
            .joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        ""
    }
}
