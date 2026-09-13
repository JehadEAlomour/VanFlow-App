package com.jehadalomour.flowvan.core.data.location

import android.content.Context

/**
 * What the heartbeat tells the server about GPS on a build that has none.
 *
 * It reports enabled, and the reason is that the server reads this field as "is
 * this rep evading tracking" and alerts an admin when it goes false. A `nogms`
 * handset is not evading anything — it was handed to them without location on
 * purpose — so reporting the device's real GPS switch would raise an alert per
 * rep per day for a fleet nobody ever intended to track.
 *
 * The [context] parameter is unused and kept only so the constructor matches the
 * `gms` actual, which needs it.
 */
@Suppress("UNUSED_PARAMETER")
actual class LocationStatusProvider(context: Context) {
    actual fun isGpsEnabled(): Boolean = true
}
