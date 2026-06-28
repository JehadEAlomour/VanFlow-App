package com.jehadalomour.flowvan.feature.map

data class NavStep(
    val instruction: String,
    val distanceText: String,
    val endLat: Double,
    val endLng: Double,
)
