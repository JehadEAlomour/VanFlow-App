package com.jehadalomour.flowvan.core.domain.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android alerts on a dedicated HIGH-importance channel that plays a loud custom
 * sound (`res/raw/order_alert.wav`) and vibrates — a heads-up banner the salesman
 * can't miss. Separate from the silent GPS-tracking channel on purpose.
 *
 * Reached while the app is running or while the tracking foreground service keeps
 * the process (and its socket) alive; there is no FCM on these handsets, so a
 * fully-killed app can't be woken — that's a known, accepted limitation.
 */
class AndroidAlertNotifier(private val context: Context) : AlertNotifier {

    private val ids = AtomicInteger(2000)
    private var channelReady = false

    override fun alert(title: String, body: String) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            ensureChannel(nm)

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
                    .setSound(soundUri())
                    .setPriority(Notification.PRIORITY_HIGH)
            }

            val notification = builder
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .build()

            nm.notify(ids.incrementAndGet(), notification)
        } catch (_: Throwable) {
            // A notification is a nicety; the in-app list already carries the message.
        }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (channelReady || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelReady = true
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تنبيهات الطلبات",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "تنبيه صوتي عند اعتماد أو تعديل طلبات البضاعة"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            setShowBadge(true)
            setSound(
                soundUri(),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        nm.createNotificationChannel(channel)
        channelReady = true
    }

    /** `res/raw/order_alert.wav`, addressed by package so no generated R is needed here. */
    private fun soundUri(): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/$SOUND_NAME")

    private companion object {
        const val CHANNEL_ID = "flowvan_alerts"
        const val SOUND_NAME = "order_alert"
    }
}
