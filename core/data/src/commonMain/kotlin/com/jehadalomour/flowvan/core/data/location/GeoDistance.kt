package com.jehadalomour.flowvan.core.data.location

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geofence radius (metres) for the per-rep location lock. Mirrors the backend
 * default `CUSTOMER_PROXIMITY_RADIUS_M` — keep the two in sync. The lock is an
 * area, not an exact point: a rep counts as "at" the customer within this radius.
 */
const val CUSTOMER_PROXIMITY_RADIUS_M: Double = 100.0

/** Great-circle distance in metres between two points (haversine). */
fun haversineMeters(a: LatLng, b: LatLng): Double {
    val r = 6_371_000.0
    val dLat = (b.lat - a.lat) * PI / 180.0
    val dLng = (b.lng - a.lng) * PI / 180.0
    val lat1 = a.lat * PI / 180.0
    val lat2 = b.lat * PI / 180.0
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * r * asin(min(1.0, sqrt(h)))
}

/** True when [rep] is within [radiusM] of [customer]. */
fun isWithinProximity(
    rep: LatLng,
    customer: LatLng,
    radiusM: Double = CUSTOMER_PROXIMITY_RADIUS_M,
): Boolean = haversineMeters(rep, customer) <= radiusM
