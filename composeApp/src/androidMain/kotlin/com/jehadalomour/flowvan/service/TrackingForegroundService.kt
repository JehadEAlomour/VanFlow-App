package com.jehadalomour.flowvan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator
import org.koin.core.context.GlobalContext

class TrackingForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY restarts this service after process death — re-arm the GPS
        // pipeline and the upload scheduler so tracking is truly always-on. No-ops
        // when called on a normal start (the coordinator is already tracking).
        val koin = GlobalContext.getOrNull()
        val userId = koin?.get<SessionStore>()?.currentUserId
        if (koin != null && !userId.isNullOrBlank()) {
            koin.get<LocationTrackingCoordinator>()
                .start(LocationTrackingCoordinator.ALWAYS_ON_SHIFT_ID, userId)
            koin.get<SyncScheduler>().start()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تتبع الموقع",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "تتبع المسار أثناء العمل"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("كاش فلو")
            .setContentText("كاش فلو يتابع المسار أثناء العمل")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "flowvan_tracking"
    }
}
