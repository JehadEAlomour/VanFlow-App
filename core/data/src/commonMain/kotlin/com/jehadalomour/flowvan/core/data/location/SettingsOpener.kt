package com.jehadalomour.flowvan.core.data.location

/**
 * Takes the rep to the screen that can actually fix their location.
 *
 * TWO DESTINATIONS, because there are two different faults and one of them
 * cannot be fixed from the other's screen:
 *
 *  - the app is not ALLOWED location → the app's own permission page;
 *  - the device's location service is OFF → the system location page, which is
 *    not reachable from the app's permission page at all.
 *
 * Sending someone to the wrong one is worse than sending them nowhere: they
 * arrive at a screen where everything already looks correct, conclude the app is
 * broken, and call the office.
 */
expect class SettingsOpener {
    /** This app's permission page, for a rep who has denied location. */
    fun openAppSettings()

    /**
     * The device's location service page, for location switched off entirely.
     *
     * Falls back to [openAppSettings] where the platform has no way in — see the
     * iOS implementation.
     */
    fun openLocationSettings()
}
