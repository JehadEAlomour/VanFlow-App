package com.jehadalomour.flowvan.core.domain.notify

/**
 * A loud, attention-grabbing OS notification — the kind a salesman standing in a
 * shop must not miss (e.g. the office just approved his stock request).
 *
 * Platform-specific: Android posts a heads-up notification on a high-importance
 * channel with a custom alert sound; iOS schedules a local notification. Bound
 * per platform in `platformModule()`.
 */
interface AlertNotifier {
    /**
     * Post an alert with sound. Best-effort and must never throw: notifications
     * are a nicety layered on top of the in-app list, and OS permission may be
     * absent. [title]/[body] are already localized by the caller.
     */
    fun alert(title: String, body: String)
}
