package com.jehadalomour.flowvan.core.data.location

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class SettingsOpener {

    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) app.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    /**
     * iOS has no public way to open Settings › Privacy › Location Services — the
     * App-Prefs URL scheme that does it is private and gets apps rejected. So
     * this lands on the app's own settings page, which is as close as Apple
     * allows, and the message that sent the rep here has to name the rest of the
     * journey rather than relying on the destination to be obvious.
     */
    actual fun openLocationSettings() = openAppSettings()
}
