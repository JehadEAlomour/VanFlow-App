package com.jehadalomour.flowvan.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.map_unavailable_in_build
import org.jetbrains.compose.resources.stringResource

/**
 * The map, in the build with no Google Maps SDK in it.
 *
 * It says so rather than drawing an empty grey rectangle, which is what a
 * GoogleMap composable renders when the SDK cannot initialise and is the single
 * most-reported "the app is broken" from handsets without Play Services.
 *
 * The screen around this is left whole on purpose — the destination name, the
 * address and the back button are all still there and all still useful. Only the
 * tiles are missing, so only the tiles are replaced.
 */
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
    Box(
        modifier = modifier.background(Color(0xFFF4F6FB)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.map_unavailable_in_build),
            modifier = Modifier.padding(32.dp),
            fontSize = 14.sp,
            color = Color(0xFF5A6B80),
            textAlign = TextAlign.Center,
        )
    }
}
