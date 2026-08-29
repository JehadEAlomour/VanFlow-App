package com.jehadalomour.flowvan.core.domain.notify

import co.touchlab.kermit.Logger

/**
 * iOS alert notifier — placeholder.
 *
 * The field handsets are Android; the iOS app is currently a bare shell with no
 * notification setup (no `UNUserNotificationCenter`, no `AppDelegate`, no APNs).
 * Wiring a real local notification here needs UserNotifications authorization +
 * a foreground-presentation delegate in `iosApp`, which is a separate task.
 *
 * Until then this logs rather than throwing, so the shared code and DI resolve
 * cleanly on iOS. The in-app request list still shows the decision on refresh.
 */
class IosAlertNotifier : AlertNotifier {
    private val log = Logger.withTag("AlertNotifier")

    override fun alert(title: String, body: String) {
        // TODO(ios): request UNUserNotificationCenter authorization and post a
        // local notification with a bundled sound (order_alert) + a foreground
        // presentation delegate in iosApp so it rings while the app is open.
        log.d { "iOS alert (not yet surfaced): $title — $body" }
    }
}
