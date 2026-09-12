package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore

/** The office requires this rep to have location on, and the phone is refusing. */
class LocationRequiredException : Exception("location permission is required")

/**
 * One rule, one place: a location-locked rep writes nothing while the phone
 * denies location.
 *
 * Sitting between the session (which knows whether THIS rep is locked, from the
 * server) and the device (which knows whether location is granted), so neither
 * the use cases nor the screens have to hold both halves. Every document goes
 * through it — sale, return, order, collection — because a rule enforced on
 * three of the four is not a rule.
 *
 * Decided on the PERMISSION, never on whether a fix arrived: a rep who has
 * allowed location and is standing inside a warehouse is not evading anything,
 * and blocking them there would be a bug the office could not explain.
 *
 * This is a courtesy, not a control. The phone is not a place to enforce
 * anything — a rep who wants to avoid it can turn location on, write, and turn
 * it off. What it does is keep an honest rep's documents located, and tell the
 * dishonest one plainly that the office is watching.
 */
class LocationGate(
    private val session: SessionStore,
    private val location: LocationProvider,
) {
    /** True when this rep may write right now. */
    fun allowed(): Boolean = !session.requireLocation || location.hasPermission()

    /** Throws [LocationRequiredException] when the rep may not write. */
    fun require() {
        if (!allowed()) throw LocationRequiredException()
    }
}
