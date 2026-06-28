package com.jehadalomour.flowvan.feature.map

import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.model.Customer

data class MapNavigationState(
    val customer: Customer? = null,
    val userLocation: LatLng? = null,
    val isLoadingLocation: Boolean = true,
    val hasCoordinates: Boolean = false,
)
