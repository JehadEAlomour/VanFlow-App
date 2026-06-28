package com.jehadalomour.flowvan.core.domain.sync

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val SYNC_INTERVAL_MS = 60_000L

class SyncScheduler(
    private val syncRepository: SyncRepository,
    private val connectivity: ConnectivityObserver,
) {
    private val log = Logger.withTag("SyncScheduler")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var reconnectJob: Job? = null

    private val _lastSyncAt = MutableStateFlow<Long?>(null)
    /** Epoch-millis of the last successful (non-skipped) sync, for the Home "last synced" chip. */
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt.asStateFlow()

    /** Periodic poll + retry-on-reconnect. Idempotent. */
    fun start() {
        if (job?.isActive != true) {
            job = scope.launch {
                while (isActive) {
                    runSync()
                    delay(SYNC_INTERVAL_MS)
                }
            }
        }
        if (reconnectJob?.isActive != true) {
            reconnectJob = connectivity.onlineEvents
                .onEach {
                    log.d("network back online — syncing pending")
                    runSync()
                }
                .launchIn(scope)
        }
    }

    /** Fire-and-forget immediate push — call right after saving a transaction. */
    fun syncNow() {
        scope.launch { runSync() }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun runSync() {
        try {
            val result = syncRepository.syncPending()
            if (!result.skipped) {
                _lastSyncAt.value = Clock.System.now().toEpochMilliseconds()
                log.d("Sync done — invoices:${result.invoicesSynced} payments:${result.paymentsSynced} points:${result.pointsSynced}")
            }
        } catch (e: Exception) {
            log.e("Sync error: ${e.message}")
        }
    }

    fun stop() {
        job?.cancel(); job = null
        reconnectJob?.cancel(); reconnectJob = null
    }
}
