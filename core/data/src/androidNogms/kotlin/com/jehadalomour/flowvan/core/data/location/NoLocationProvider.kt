package com.jehadalomour.flowvan.core.data.location

/**
 * The `nogms` build's answer to "where is this rep": it does not know, and it
 * never will.
 *
 * The app is built without location permissions, so there is nothing to ask and
 * nothing that could be granted — this is not a provider that failed, it is a
 * build that has no location in it.
 *
 * WHY THE TWO PREDICATES SAY YES. [hasPermission] and [isServiceEnabled] are not
 * "does the device have a fix", they are "is this rep refusing to be located" —
 * that is the question LocationGate and the LocationLock screen ask them, and
 * they act on a no by refusing every document and shutting the app. Answering no
 * here would brick the build for any rep the office has ticked as
 * location-required: a wall they cannot climb, because the settings screen the
 * lock sends them to has no entry for an app that asked for no permission. So
 * the gate is told there is nothing wrong with the phone — which is true, the
 * phone is fine — and the office enforces location by shipping the `gms` build
 * to the fleets it wants located.
 *
 * [lastLocation] still returns null, because that one IS a question about a fix
 * and every caller already handles not having one: documents are simply written
 * without coordinates, exactly as they are on a `gms` handset indoors.
 */
class NoLocationProvider : LocationProvider {
    override suspend fun lastLocation(): LatLng? = null
    override fun hasPermission(): Boolean = true
    override fun isServiceEnabled(): Boolean = true
}
