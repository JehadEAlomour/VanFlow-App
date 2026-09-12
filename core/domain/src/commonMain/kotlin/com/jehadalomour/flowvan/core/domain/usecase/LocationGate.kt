package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore

/**
 * Why a location-locked rep is being stopped — and therefore which screen will
 * fix it.
 *
 * The two faults are not interchangeable: a rep who has switched location off
 * device-wide cannot fix it on the app's permission page, where everything
 * already looks correct.
 */
enum class LocationBlock {
    /** Nothing is wrong; the rep may write. */
    NONE,

    /** The rep has not allowed this app to use location. */
    PERMISSION_DENIED,

    /** Location is allowed, but the device's location service is switched off. */
    SERVICE_OFF,
}

/** The office requires this rep to have location on, and the phone is refusing. */
class LocationRequiredException(
    val block: LocationBlock = LocationBlock.PERMISSION_DENIED,
) : Exception("location is required ($block)")

/**
 * One rule, one place: a location-locked rep writes nothing while the phone
 * refuses location.
 *
 * Sitting between the session (which knows whether THIS rep is locked, from the
 * server) and the device (which knows what location is doing), so neither the
 * use cases nor the screens have to hold both halves. Every document goes
 * through it — sale, return, order, collection — because a rule enforced on
 * three of the four is not a rule.
 *
 * BOTH HALVES OF "LOCATION IS ON" ARE CHECKED. The permission alone was not
 * enough and the gap was invisible: a rep could grant the app location, switch
 * the device's location service off, and go on writing all day with the office
 * believing the requirement was in force. Granting permission is not the same as
 * having location on, and only the second one produces a position.
 *
 * Still decided on what the device is CONFIGURED to do, never on whether a fix
 * has actually arrived: a rep who has location on and is standing inside a
 * concrete warehouse is not evading anything, and blocking them there would be a
 * bug the office could not explain.
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
    /** What, if anything, is stopping this rep right now. */
    fun check(): LocationBlock = when {
        !session.requireLocation -> LocationBlock.NONE
        // Permission first: it is the one the rep controls from the app, and a
        // device with location off usually reports no permission problem at all,
        // so checking it second would report the harder fault for the easy fault.
        !location.hasPermission() -> LocationBlock.PERMISSION_DENIED
        !location.isServiceEnabled() -> LocationBlock.SERVICE_OFF
        else -> LocationBlock.NONE
    }

    /** True when this rep may write right now. */
    fun allowed(): Boolean = check() == LocationBlock.NONE

    /** Throws [LocationRequiredException], carrying which fault it was. */
    fun require() {
        val block = check()
        if (block != LocationBlock.NONE) throw LocationRequiredException(block)
    }
}
