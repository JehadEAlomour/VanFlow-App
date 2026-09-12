package com.jehadalomour.flowvan.core.data.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

actual class SettingsOpener(private val context: Context) {

    actual fun openAppSettings() {
        start(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    actual fun openLocationSettings() {
        start(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    /**
     * NEW_TASK because this is started from an application context, not an
     * Activity — without it Android throws rather than opening anything.
     *
     * And every launch is guarded: a device with the target screen removed (some
     * heavily customised ROMs do this) would otherwise crash the app while the
     * rep was trying to follow our own instructions. Failing to open a settings
     * page is a nuisance; crashing on the way to it is worse than saying nothing.
     */
    private fun start(intent: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
