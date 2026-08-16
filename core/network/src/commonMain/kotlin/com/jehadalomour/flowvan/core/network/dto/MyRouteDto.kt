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
    /**
     * Days of the rep's route cycle this outlet is visited, each 0..cycleDays-1.
     * On the default 7-day cycle these are weekdays (0=Sunday); on a 14-day
     * cycle they run 0..13 and a weekday appears twice.
     */
    val cycleDays: List<Int> = emptyList(),
    /** Legacy name for the same values; older servers send only this. */
    val weekdays: List<Int> = emptyList(),
    val note: String? = null,
    val todo: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val todoDoneToday: Boolean = false,
) {
    /**
     * The scheduled days, whichever field the server populated. Reading this
     * rather than either field directly means the handset works against a
     * server from before route cycles and one from after.
     */
    val days: List<Int> get() = cycleDays.ifEmpty { weekdays }
}

/** A salesman's route cycle: how long it runs and where today sits in it. */
@Serializable
data class RouteCycleDto(
    val cycleDays: Int = 7,
    /** YYYY-MM-DD that counts as day 0. */
    val anchorDate: String = "",
    val name: String? = null,
    /** Which day of the cycle today falls on. */
    val todayIndex: Int = 0,
)
