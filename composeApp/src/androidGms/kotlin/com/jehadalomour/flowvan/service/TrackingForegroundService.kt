package com.jehadalomour.flowvan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.jehadalomour.flowvan.core.data.heartbeat.HeartbeatReporter
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val GPS_CHANGE_DEBOUNCE_MS = 800L

class TrackingForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gpsDebounceJob: Job? = null

    /**
     * Fires the moment the device's location providers change (GPS toggled) so the
     * server learns about a GPS on/off within a second, instead of waiting for the
     * 60s sync cycle. Android emits PROVIDERS_CHANGED several times per toggle (one
     * per provider), so we debounce to collapse the burst into a single heartbeat.
     */
    private val gpsChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            gpsDebounceJob?.cancel()
            gpsDebounceJob = scope.launch {
                delay(GPS_CHANGE_DEBOUNCE_MS)
                GlobalContext.getOrNull()?.get<HeartbeatReporter>()?.send()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        registerGpsChangeReceiver()
    }

    private fun registerGpsChangeReceiver() {
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gpsChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(gpsChangeReceiver, filter)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        // The rep swiped the app away. The foreground service keeps tracking, but
        // report an immediate "app closed" heartbeat (best-effort, one-shot) so the
        // admin is alerted right away instead of waiting for the offline watchdog.
        GlobalContext.getOrNull()?.get<HeartbeatReporter>()?.let { reporter ->
            scope.launch { reporter.send(appState = "closed") }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(gpsChangeReceiver) }
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
