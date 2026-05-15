package com.jehadalomour.flowvan.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.presentation.feature.map.MapNavigationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MapNavigationScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: MapNavigationViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val customer = state.customer
    var driveDuration by remember { mutableStateOf("") }
    var driveDistance by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text("🗺️", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        customer?.nameAr ?: "الملاحة",
                        color = Fv.TextHigh,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    customer?.area?.let { Text(it, color = Fv.TextMid, fontSize = 11.sp) }
                }
            }

            when {
                !state.hasCoordinates && customer != null -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "لا تتوفر إحداثيات لهذا العميل",
                                color = Fv.TextMid,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                customer?.lat != null && customer.lng != null -> {
                    PlatformMapContent(
                        userLat = state.userLocation?.lat,
                        userLng = state.userLocation?.lng,
                        customerLat = customer.lat!!,
                        customerLng = customer.lng!!,
                        customerName = customer.nameAr,
                        modifier = Modifier.weight(1f),
                        onRouteInfo = { dur, dist -> driveDuration = dur; driveDistance = dist },
                    )
                }

                else -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Fv.Blue)
                    }
                }
            }

            customer?.let { customer ->
                if (customer.lat != null && customer.lng != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(customer.nameAr, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            customer.addressAr?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Fv.TextMid, fontSize = 12.sp)
                            }
                            customer.phone?.let {
                                Text("📞 $it", color = Fv.TextMid, fontSize = 12.sp)
                            }
                            if (driveDuration.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                                    Text("🕐 $driveDuration", color = Fv.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("📍 $driveDistance", color = Fv.TextMid, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            if (state.isLoadingLocation) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Fv.Blue, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("جارٍ تحديد موقعك...", color = Fv.TextMid, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val dest = "${customer.lat},${customer.lng}"
                                        val url = "https://www.google.com/maps/dir/?api=1&destination=$dest&travelmode=driving"
                                        uriHandler.openUri(url)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Fv.Blue,
                                        contentColor = Fv.TextHigh,
                                    ),
                                ) {
                                    Text("🚗  ابدأ الملاحة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
