package com.jehadalomour.flowvan.shared.presentation.feature.map

import com.jehadalomour.flowvan.shared.data.location.LatLng
import com.jehadalomour.flowvan.shared.domain.model.Customer

data class MapNavigationState(
    val customer: Customer? = null,
    val userLocation: LatLng? = null,
    val isLoadingLocation: Boolean = true,
    val hasCoordinates: Boolean = false,
)
