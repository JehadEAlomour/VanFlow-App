package com.jehadalomour.flowvan.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformMapContent(
    userLat: Double?,
    userLng: Double?,
    customerLat: Double,
    customerLng: Double,
    customerName: String,
    modifier: Modifier = Modifier,
    isNavigating: Boolean = false,
    onRouteInfo: (duration: String, distance: String) -> Unit = { _, _ -> },
    onStepsLoaded: (List<NavStep>) -> Unit = {},
    onLocationUpdate: (lat: Double, lng: Double) -> Unit = { _, _ -> },
)
