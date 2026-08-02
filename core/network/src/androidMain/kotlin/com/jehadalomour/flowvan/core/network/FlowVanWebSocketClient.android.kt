package com.jehadalomour.flowvan.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

/** No HttpTimeout here on purpose — see the expect declaration. */
actual fun createWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
}
