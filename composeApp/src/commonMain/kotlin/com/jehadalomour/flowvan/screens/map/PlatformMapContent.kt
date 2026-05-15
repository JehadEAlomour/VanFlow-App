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
)
