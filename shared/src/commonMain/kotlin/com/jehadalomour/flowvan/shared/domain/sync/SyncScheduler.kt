package com.jehadalomour.flowvan.shared.domain.sync

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.shared.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SYNC_INTERVAL_MS = 60_000L

class SyncScheduler(private val syncRepository: SyncRepository) {
    private val log = Logger.withTag("SyncScheduler")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                try {
                    val result = syncRepository.syncPending()
                    if (!result.skipped) {
                        log.d("Sync done — invoices:${result.invoicesSynced} payments:${result.paymentsSynced} points:${result.pointsSynced}")
                    }
                } catch (e: Exception) {
                    log.e("Sync error: ${e.message}")
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
