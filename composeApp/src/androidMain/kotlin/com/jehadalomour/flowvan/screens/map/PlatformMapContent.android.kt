package com.jehadalomour.flowvan.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun PlatformMapContent(
    userLat: Double?,
    userLng: Double?,
    customerLat: Double,
    customerLng: Double,
    customerName: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    val customerLatLng = LatLng(customerLat, customerLng)
    val userLatLng = if (userLat != null && userLng != null) LatLng(userLat, userLng) else null

    val cameraCenter = if (userLatLng != null) {
        LatLng((customerLat + userLat!!) / 2.0, (customerLng + userLng!!) / 2.0)
    } else customerLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraCenter, if (userLatLng != null) 12f else 14f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = true,
            compassEnabled = true,
        ),
    ) {
        Marker(
            state = MarkerState(position = customerLatLng),
            title = customerName,
            snippet = "الوجهة",
        )

        if (userLatLng != null) {
            Polyline(
                points = listOf(userLatLng, customerLatLng),
                color = Color(0xFF4B8FF6),
                width = 8f,
                geodesic = true,
            )
        }
    }
}
