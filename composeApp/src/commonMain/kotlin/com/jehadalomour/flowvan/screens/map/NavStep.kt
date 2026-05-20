package com.jehadalomour.flowvan.screens.map

data class NavStep(
    val instruction: String,
    val distanceText: String,
    val endLat: Double,
    val endLng: Double,
)
