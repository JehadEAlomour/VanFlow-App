package com.jehadalomour.flowvan.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.feature.map.MapNavigationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Navigate to a bare point — a prospect found in customer search that is not a
 * customer yet. Same screen, a Point instead of an id.
 */
@Composable
fun MapNavigationScreen(
    lat: Double,
    lng: Double,
    label: String,
    onBack: () -> Unit,
) {
    val point = remember(lat, lng, label) { MapNavigationViewModel.Point(lat, lng, label) }
    val viewModel: MapNavigationViewModel = koinViewModel { parametersOf(point) }
    MapNavigationContent(viewModel = viewModel, onBack = onBack)
}

@Composable
fun MapNavigationScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: MapNavigationViewModel = koinViewModel { parametersOf(customerId) },
) {
    MapNavigationContent(viewModel = viewModel, onBack = onBack)
}

@Composable
private fun MapNavigationContent(
    viewModel: MapNavigationViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val customer = state.customer
    var driveDuration by remember { mutableStateOf("") }
    var driveDistance by remember { mutableStateOf("") }
    var isNavigating by remember { mutableStateOf(false) }
    var navSteps by remember { mutableStateOf<List<NavStep>>(emptyList()) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var navUserLat by remember { mutableStateOf<Double?>(null) }
    var navUserLng by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(navUserLat, navUserLng) {
        if (!isNavigating) return@LaunchedEffect
        val lat = navUserLat ?: return@LaunchedEffect
        val lng = navUserLng ?: return@LaunchedEffect
        val step = navSteps.getOrNull(currentStepIndex) ?: return@LaunchedEffect
        if (haversineMeters(lat, lng, step.endLat, step.endLng) < 40.0) {
            if (currentStepIndex < navSteps.lastIndex) currentStepIndex++
            else isNavigating = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (isNavigating) isNavigating = false else onBack() }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Icon(
                    painter = painterResource(Res.drawable.ic_map),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        state.destName.ifEmpty { stringResource(Res.string.map_navigation_fallback) },
                        color = Fv.TextHigh,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    customer?.area?.let { Text(it, color = Fv.TextMid, fontSize = 11.sp) }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    !state.hasCoordinates && customer != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_map),
                                    contentDescription = null,
                                    tint = Fv.TextMid,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(Res.string.map_no_location),
                                    color = Fv.TextMid,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }

                    state.destLat != null && state.destLng != null -> {
                        PlatformMapContent(
                            userLat = state.userLocation?.lat,
                            userLng = state.userLocation?.lng,
                            customerLat = state.destLat!!,
                            customerLng = state.destLng!!,
                            customerName = state.destName,
                            modifier = Modifier.fillMaxSize(),
                            isNavigating = isNavigating,
                            onRouteInfo = { dur, dist -> driveDuration = dur; driveDistance = dist },
                            onStepsLoaded = { steps -> navSteps = steps; currentStepIndex = 0 },
                            onLocationUpdate = { lat, lng -> navUserLat = lat; navUserLng = lng },
                        )
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fv.Blue)
                        }
                    }
                }

                if (isNavigating && navSteps.isNotEmpty()) {
                    val step = navSteps.getOrNull(currentStepIndex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_directions_car),
                                    contentDescription = null,
                                    tint = Fv.Blue,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    step?.instruction ?: "",
                                    color = Fv.TextHigh,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            step?.distanceText?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Fv.TextMid, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(Res.string.map_step_of, currentStepIndex + 1, navSteps.size),
                                color = Fv.TextMid,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }

            customer?.let { c ->
                if (c.lat != null && c.lng != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!isNavigating) {
                                Text(c.nameAr, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                c.addressAr?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, color = Fv.TextMid, fontSize = 12.sp)
                                }
                                c.phone?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_call),
                                            contentDescription = null,
                                            tint = Fv.TextMid,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(it, color = Fv.TextMid, fontSize = 12.sp)
                                    }
                                }
                                if (driveDuration.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_alarm),
                                                contentDescription = null,
                                                tint = Fv.Green,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(driveDuration, color = Fv.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_map),
                                                contentDescription = null,
                                                tint = Fv.TextMid,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(driveDistance, color = Fv.TextMid, fontSize = 13.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            if (state.isLoadingLocation && !isNavigating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(16.dp).width(16.dp),
                                        color = Fv.Blue,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(Res.string.map_locating), color = Fv.TextMid, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (isNavigating) {
                                            isNavigating = false
                                        } else {
                                            currentStepIndex = 0
                                            isNavigating = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNavigating) Fv.Red else Fv.Blue,
                                        contentColor = Fv.TextHigh,
                                    ),
                                ) {
                                    if (isNavigating) {
                                        Text(stringResource(Res.string.map_stop_navigation), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    } else {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_directions_car),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(Res.string.map_start_navigation), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dphi = Math.toRadians(lat2 - lat1)
    val dlam = Math.toRadians(lng2 - lng1)
    val a = sin(dphi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dlam / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}
