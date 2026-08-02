package com.jehadalomour.flowvan.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

/** No HttpTimeout here on purpose — see the expect declaration. */
actual fun createWebSocketClient(): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
}
