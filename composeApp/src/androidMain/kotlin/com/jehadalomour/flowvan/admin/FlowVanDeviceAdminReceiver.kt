package com.jehadalomour.flowvan.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger

/**
 * The component that makes FlowVan the device owner.
 *
 * It has no behaviour, and that is the point — it exists so there is a
 * `<receiver>` for `adb shell dpm set-device-owner` to name. Being device owner
 * is what lets [com.jehadalomour.flowvan.core.data.update.AppInstaller] commit
 * an install with no dialog, which is the difference between a fleet that
 * updates itself and one that needs a salesman to understand a system prompt.
 *
 * It can only be granted on a handset with no accounts added yet — in practice,
 * straight out of the box or straight after a factory reset, before the device
 * is handed over. There is no way to grant it later. See docs/auto-update.md.
 */
class FlowVanDeviceAdminReceiver : DeviceAdminReceiver() {

    private val log = Logger.withTag("AppUpdate")

    override fun onEnabled(context: Context, intent: Intent) {
        log.i { "device admin enabled — silent updates are available on this handset" }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Worth a line in the log, because it is invisible from the outside and
        // it silently converts this handset from zero-tap to one-tap updates.
        log.w { "device admin disabled — updates will need the salesman to confirm" }
    }
}
