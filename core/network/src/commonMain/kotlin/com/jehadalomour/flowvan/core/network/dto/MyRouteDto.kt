package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * One outlet on the salesman's recurring route for a given day, with the admin
 * note + to-do attached to the journey-plan entry. Mirrors the backend
 * `JourneyPlanRow`.
 */
@Serializable
data class MyRouteStopDto(
    val id: String,
    val customerId: String,
    val customerNumber: String,
    val customerName: String,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val city: String? = null,
    val addressAr: String? = null,
    val phone: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    /** 0=Sunday .. 6=Saturday */
    val weekdays: List<Int> = emptyList(),
    val note: String? = null,
    val todo: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val todoDoneToday: Boolean = false,
)
