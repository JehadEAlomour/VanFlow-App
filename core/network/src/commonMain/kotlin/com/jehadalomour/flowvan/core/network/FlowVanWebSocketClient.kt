package com.jehadalomour.flowvan.core.network

import io.ktor.client.HttpClient

/**
 * A SEPARATE client for the realtime socket.
 *
 * The main [createHttpClient] installs HttpTimeout with a 15s request timeout so
 * a dead network fails fast and callers drop to the offline cache. A WebSocket
 * is meant to stay open for hours — reusing that client would have the socket
 * torn down every 15 seconds, and the reconnect loop would hide it as a flaky
 * network rather than a self-inflicted timeout.
 */
expect fun createWebSocketClient(): HttpClient
