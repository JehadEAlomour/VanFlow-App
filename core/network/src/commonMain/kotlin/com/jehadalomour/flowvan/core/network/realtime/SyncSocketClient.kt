package com.jehadalomour.flowvan.core.network.realtime

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/** Which slice of local data the server says has gone stale. */
enum class SyncResource { OFFERS, CUSTOMERS, STOCK }

/**
 * Listens for the backend's "your data changed" signals and republishes them.
 *
 * Deliberately does NOT refresh anything itself — it has no business knowing how
 * a catalog is pulled. It reports what the server said; the domain layer decides
 * what to do about it.
 *
 * ## Connection behaviour
 *
 * A van is offline more often than it is online, so a dropped socket is normal
 * operation, not an error. The loop reconnects with capped exponential backoff
 * and logs at debug, so a day in a basement does not fill the log with warnings.
 *
 * Nothing is buffered across a disconnect, by design: the signal is only a hint
 * to pull, and the app already pulls on foreground. A rep who was offline for an
 * hour reconnects and pulls once, rather than replaying an hour of stale nudges.
 */
class SyncSocketClient(
    private val http: HttpClient,
    private val apiConfig: ApiConfig,
    private val session: SessionStore,
) {
    private val log = Logger.withTag("SyncSocket")

    private val _signals = MutableSharedFlow<SyncResource>(extraBufferCapacity = 16)
    /** Resources the server has told us to re-pull. */
    val signals: SharedFlow<SyncResource> = _signals.asSharedFlow()

    private var job: Job? = null

    /** Idempotent: calling start() while already connected does nothing. */
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch { runLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun runLoop() {
        var attempt = 0
        while (currentCoroutineContextIsActive()) {
            val token = session.currentToken
            if (token.isNullOrBlank() || !apiConfig.isEnabled) {
                // Not signed in, or running offline-only. Nothing to connect to;
                // check back rather than spinning.
                delay(RETRY_WHEN_IDLE_MS)
                continue
            }
            try {
                connectOnce(token)
                attempt = 0 // a clean session resets the backoff
            } catch (e: Exception) {
                // Expected on a van: tunnels, dead spots, server restarts. Debug,
                // not warn — this is the normal state of a moving vehicle.
                log.d { "socket dropped: ${e.message}" }
            }
            attempt = min(attempt + 1, MAX_BACKOFF_STEPS)
            delay(backoffMs(attempt))
        }
    }

    private suspend fun connectOnce(token: String) {
        val url = websocketUrl()
        log.d { "connecting $url" }
        val socket = http.webSocketSession(url)
        try {
            for (frame in socket.incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                when (val parsed = parseSocketIoFrame(text, NAMESPACE)) {
                    SocketIoIn.Open -> socket.send(SocketIoFrames.connect(NAMESPACE, token))
                    // The server pings us; failing to pong gets the socket culled.
                    SocketIoIn.Ping -> socket.send(SocketIoFrames.ENGINE_PONG.toString())
                    SocketIoIn.Connected -> log.d { "connected to $NAMESPACE" }
                    SocketIoIn.Closed -> return
                    is SocketIoIn.ConnectError -> {
                        // Almost always an expired token. Reconnecting with the same
                        // one would loop, so back out and let the next attempt read
                        // whatever the session holds by then.
                        log.d { "connect rejected: ${parsed.detail}" }
                        return
                    }
                    is SocketIoIn.Event -> onEvent(parsed)
                    is SocketIoIn.Ignored -> Unit
                }
            }
        } finally {
            runCatching { socket.close() }
        }
    }

    private suspend fun onEvent(event: SocketIoIn.Event) {
        if (event.name != SYNC_EVENT) return
        val resource = resourceOf(event.payloadJson) ?: run {
            // A resource this build does not know about — a newer server. Ignore it
            // rather than guessing; a wrong guess would refresh the wrong thing.
            log.d { "unknown sync resource in ${event.payloadJson}" }
            return
        }
        log.d { "sync signal: $resource" }
        _signals.emit(resource)
    }

    /**
     * Pull the resource out of `{"resource":"offers","reason":…,"at":…}` without a
     * full JSON parse — the payload is fixed and tiny, and this keeps the network
     * module free of a serializer dependency for one field.
     */
    private fun resourceOf(json: String): SyncResource? {
        val key = "\"resource\""
        val at = json.indexOf(key)
        if (at < 0) return null
        val colon = json.indexOf(':', at + key.length)
        if (colon < 0) return null
        val open = json.indexOf('"', colon)
        if (open < 0) return null
        val close = json.indexOf('"', open + 1)
        if (close < 0) return null
        return when (json.substring(open + 1, close).lowercase()) {
            "offers" -> SyncResource.OFFERS
            "customers" -> SyncResource.CUSTOMERS
            "stock" -> SyncResource.STOCK
            else -> null
        }
    }

    /**
     * The API base ends in `/api/v1`; the socket lives at the server ROOT, so the
     * prefix is stripped rather than appended to.
     */
    private fun websocketUrl(): String {
        val root = apiConfig.resolvedBaseUrl
            .trimEnd('/')
            .removeSuffix("/${ApiConfig.API_PREFIX}")
            .trimEnd('/')
        val ws = when {
            root.startsWith("https://") -> "wss://" + root.removePrefix("https://")
            root.startsWith("http://") -> "ws://" + root.removePrefix("http://")
            else -> root
        }
        return "$ws/socket.io/?EIO=4&transport=websocket"
    }

    private fun backoffMs(attempt: Int): Long =
        min(BASE_BACKOFF_MS shl (attempt - 1), MAX_BACKOFF_MS)

    private companion object {
        const val NAMESPACE = "/ws/ops"
        const val SYNC_EVENT = "sync.required"
        const val BASE_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
        const val MAX_BACKOFF_STEPS = 5
        const val RETRY_WHEN_IDLE_MS = 10_000L
    }
}

private suspend fun currentCoroutineContextIsActive(): Boolean =
    kotlin.coroutines.coroutineContext[Job]?.isActive ?: true
