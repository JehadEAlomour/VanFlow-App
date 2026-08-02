package com.jehadalomour.flowvan.core.domain.sync

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.domain.usecase.RefreshCatalogUseCase
import com.jehadalomour.flowvan.core.network.realtime.SyncResource
import com.jehadalomour.flowvan.core.network.realtime.SyncSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Turns the server's "your data changed" signals into actual refreshes.
 *
 * The socket client only reports; this decides what to do. Keeping the two apart
 * means the transport can be swapped without touching refresh logic, and the
 * refresh can be tested without a socket.
 *
 * Two safeguards, both learned from how these signals actually arrive:
 *
 *  - **Debounced.** Editing an offer in the dashboard saves several times in a
 *    row, and an ERP pull can touch hundreds of customers. Without a pause the
 *    van would re-pull the catalog once per keystroke of someone else's typing.
 *  - **Serialised.** One refresh at a time. Two overlapping pulls of the same
 *    resource race on the same tables, and the loser can overwrite the winner
 *    with older rows.
 */
class RealtimeSyncCoordinator(
    private val socket: SyncSocketClient,
    private val refreshCatalog: RefreshCatalogUseCase,
) {
    private val log = Logger.withTag("RealtimeSync")
    private val lock = Mutex()
    private var job: Job? = null

    /**
     * Its own scope, like SyncScheduler's — NOT a viewModelScope. The socket has
     * to outlive the Home screen, or walking into the voucher screen would drop
     * the connection and the rep would stop hearing about changes exactly while
     * selling.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Idempotent — safe to call on every login or app foreground. */
    fun start() {
        if (job?.isActive == true) return
        socket.start(scope)
        job = scope.launch {
            socket.signals.collect { resource ->
                launch { handle(resource) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        socket.stop()
    }

    private suspend fun handle(resource: SyncResource) {
        delay(DEBOUNCE_MS)
        // Serialised rather than skipped: whoever waits still runs, because the
        // last signal is the one that reflects the final state.
        lock.withLock {
            log.d { "refreshing $resource after signal" }
            refreshCatalog.refreshOnly(resource)
        }
    }

    private companion object {
        /**
         * Long enough to swallow a burst of edits, short enough that a rep who is
         * told "I've added the customer" sees it before they can ask again.
         */
        const val DEBOUNCE_MS = 1_500L
    }
}
