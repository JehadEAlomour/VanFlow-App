package com.jehadalomour.flowvan.core.network.realtime

/**
 * The slice of the Socket.IO v4 (Engine.IO 4) wire protocol this app needs.
 *
 * Socket.IO is NOT plain WebSocket — it layers its own text framing on top, and
 * there is no Kotlin Multiplatform client for it. Rather than pull an
 * Android-only library (and leave iOS unable to build) or ask the backend for a
 * second, plain-WebSocket gateway, the handful of frames we actually exchange
 * are parsed here.
 *
 * Frames used, in the order they occur:
 *
 *   server  0{"sid":…,"pingInterval":25000,…}   engine OPEN
 *   client  40/ws/ops,{"token":"<jwt>"}         namespace CONNECT, with auth
 *   server  40/ws/ops,{"sid":…}                 CONNECT accepted
 *   server  2                                   PING   →  client answers 3 (PONG)
 *   server  42/ws/ops,["sync.required",{…}]     EVENT
 *   server  44/ws/ops,{…}                       CONNECT_ERROR (auth rejected)
 *
 * The token travels in the CONNECT payload rather than the query string. The
 * server accepts either, but a JWT in a URL ends up in proxy and access logs;
 * in a frame body it does not.
 */
internal object SocketIoFrames {
    const val ENGINE_OPEN = '0'
    const val ENGINE_CLOSE = '1'
    const val ENGINE_PING = '2'
    const val ENGINE_PONG = '3'
    const val ENGINE_MESSAGE = '4'

    /** Socket.IO packet types, which appear as the SECOND character (after '4'). */
    const val PACKET_CONNECT = '0'
    const val PACKET_EVENT = '2'
    const val PACKET_CONNECT_ERROR = '4'

    /** `40/ws/ops,{"token":"…"}` — connect to the namespace, carrying auth. */
    fun connect(namespace: String, token: String): String =
        "$ENGINE_MESSAGE$PACKET_CONNECT$namespace,{\"token\":${quote(token)}}"

    /** `41/ws/ops` — leave the namespace politely before closing. */
    fun disconnect(namespace: String): String = "${ENGINE_MESSAGE}1$namespace"

    private fun quote(raw: String): String {
        val sb = StringBuilder(raw.length + 2)
        sb.append('"')
        for (c in raw) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}

/** What a received frame turned out to be. */
internal sealed interface SocketIoIn {
    data object Open : SocketIoIn
    data object Ping : SocketIoIn
    data object Closed : SocketIoIn
    data object Connected : SocketIoIn
    data class ConnectError(val detail: String) : SocketIoIn

    /** An application event: name plus its first argument, still raw JSON. */
    data class Event(val name: String, val payloadJson: String) : SocketIoIn
    data class Ignored(val raw: String) : SocketIoIn
}

/**
 * Parse one inbound text frame.
 *
 * Only frames for [namespace] are surfaced as events — the server may serve
 * other namespaces on the same connection, and acting on their traffic would be
 * both wrong and, for a rep, potentially another van's data.
 */
internal fun parseSocketIoFrame(raw: String, namespace: String): SocketIoIn {
    if (raw.isEmpty()) return SocketIoIn.Ignored(raw)
    when (raw[0]) {
        SocketIoFrames.ENGINE_OPEN -> return SocketIoIn.Open
        SocketIoFrames.ENGINE_PING -> return SocketIoIn.Ping
        SocketIoFrames.ENGINE_CLOSE -> return SocketIoIn.Closed
        SocketIoFrames.ENGINE_MESSAGE -> Unit
        else -> return SocketIoIn.Ignored(raw)
    }
    if (raw.length < 2) return SocketIoIn.Ignored(raw)

    val body = raw.substring(2)
    return when (raw[1]) {
        SocketIoFrames.PACKET_CONNECT ->
            if (matchesNamespace(body, namespace)) SocketIoIn.Connected
            else SocketIoIn.Ignored(raw)

        SocketIoFrames.PACKET_CONNECT_ERROR ->
            SocketIoIn.ConnectError(body.substringAfter(',', body))

        SocketIoFrames.PACKET_EVENT -> parseEvent(body, namespace)
        else -> SocketIoIn.Ignored(raw)
    }
}

private fun matchesNamespace(body: String, namespace: String): Boolean =
    body.startsWith(namespace) &&
        (body.length == namespace.length || body[namespace.length] == ',')

/**
 * `/ws/ops,["sync.required",{…}]` — and the namespace may be followed by an ack
 * id (`/ws/ops,17[…]`) which we neither need nor answer.
 */
private fun parseEvent(body: String, namespace: String): SocketIoIn {
    if (!matchesNamespace(body, namespace)) return SocketIoIn.Ignored(body)
    val afterNs = body.substring(minOf(namespace.length + 1, body.length))
    val arrayStart = afterNs.indexOf('[')
    if (arrayStart < 0) return SocketIoIn.Ignored(body)
    // Anything between the namespace and '[' is an ack id; skipping it is safe
    // because we never ack.
    val array = afterNs.substring(arrayStart)

    val firstQuote = array.indexOf('"')
    if (firstQuote < 0) return SocketIoIn.Ignored(body)
    val closingQuote = array.indexOf('"', firstQuote + 1)
    if (closingQuote < 0) return SocketIoIn.Ignored(body)
    val name = array.substring(firstQuote + 1, closingQuote)

    val comma = array.indexOf(',', closingQuote)
    val payload =
        if (comma < 0) "{}"
        else array.substring(comma + 1).trimEnd().removeSuffix("]").trim()

    return SocketIoIn.Event(name, payload.ifBlank { "{}" })
}
