package com.jehadalomour.flowvan.feature.map

import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.model.Customer

/**
 * The map can guide to a saved customer OR to a bare point found in the
 * customer search, so the destination is expressed once — lat/lng/name — and
 * derived from whichever source loaded it. The screen reads only the derived
 * fields and never has to know which it was.
 */
data class MapNavigationState(
    val customer: Customer? = null,
    /** A raw destination, set when navigating to a prospect that has no customer row yet. */
    val pointLat: Double? = null,
    val pointLng: Double? = null,
    val pointLabel: String? = null,
    val userLocation: LatLng? = null,
    val isLoadingLocation: Boolean = true,
    val hasCoordinates: Boolean = false,
) {
    val destLat: Double? get() = customer?.lat ?: pointLat
    val destLng: Double? get() = customer?.lng ?: pointLng
    val destName: String get() = customer?.nameAr ?: pointLabel ?: ""
}
