package com.jehadalomour.flowvan.core.network.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Socket.IO framing is hand-rolled because no KMP client exists, so it is
 * pinned here against real frames. A parser bug here is invisible in testing —
 * the socket connects, nothing arrives, and the app just looks "not real-time".
 */
class SocketIoFramesTest {

    private val ns = "/ws/ops"

    @Test
    fun engineOpenIsRecognised() {
        val open = parseSocketIoFrame("""0{"sid":"abc","pingInterval":25000}""", ns)
        assertTrue(open is SocketIoIn.Open)
    }

    @Test
    fun pingIsRecognisedSoWeCanPong() {
        assertTrue(parseSocketIoFrame("2", ns) is SocketIoIn.Ping)
    }

    @Test
    fun namespaceConnectAcceptedIsRecognised() {
        assertTrue(parseSocketIoFrame("""40/ws/ops,{"sid":"xyz"}""", ns) is SocketIoIn.Connected)
    }

    @Test
    fun connectFrameCarriesTheTokenInTheBodyNotTheUrl() {
        val frame = SocketIoFrames.connect(ns, "jwt.value.here")
        assertEquals("""40/ws/ops,{"token":"jwt.value.here"}""", frame)
    }

    @Test
    fun eventIsParsedWithItsPayload() {
        val ev = parseSocketIoFrame(
            """42/ws/ops,["sync.required",{"resource":"offers","reason":"offer.updated","at":"2026-08-02T10:00:00.000Z"}]""",
            ns,
        )
        assertTrue(ev is SocketIoIn.Event)
        assertEquals("sync.required", ev.name)
        assertTrue(ev.payloadJson.contains("\"resource\":\"offers\""))
    }

    /** Socket.IO may insert an ack id between namespace and payload; we never ack. */
    @Test
    fun eventWithAnAckIdStillParses() {
        val ev = parseSocketIoFrame(
            """42/ws/ops,17["sync.required",{"resource":"stock"}]""",
            ns,
        )
        assertTrue(ev is SocketIoIn.Event)
        assertEquals("sync.required", ev.name)
        assertTrue(ev.payloadJson.contains("stock"))
    }

    /**
     * Another namespace's traffic must never surface as ours — for a rep that
     * could mean acting on a different van's signal.
     */
    @Test
    fun otherNamespacesAreIgnored() {
        assertTrue(
            parseSocketIoFrame("""42/other,["sync.required",{"resource":"offers"}]""", ns)
                is SocketIoIn.Ignored,
        )
        assertTrue(parseSocketIoFrame("""40/other,{"sid":"x"}""", ns) is SocketIoIn.Ignored)
    }

    /** A namespace that merely starts with ours is a different namespace. */
    @Test
    fun namespacePrefixIsNotAMatch() {
        assertTrue(
            parseSocketIoFrame("""42/ws/opsX,["sync.required",{}]""", ns) is SocketIoIn.Ignored,
        )
    }

    @Test
    fun connectErrorIsSurfacedSoWeDoNotLoop() {
        val err = parseSocketIoFrame("""44/ws/ops,{"message":"Invalid token"}""", ns)
        assertTrue(err is SocketIoIn.ConnectError)
        assertTrue(err.detail.contains("Invalid token"))
    }

    @Test
    fun engineCloseEndsTheSession() {
        assertTrue(parseSocketIoFrame("1", ns) is SocketIoIn.Closed)
    }

    @Test
    fun junkIsIgnoredRatherThanCrashing() {
        assertTrue(parseSocketIoFrame("", ns) is SocketIoIn.Ignored)
        assertTrue(parseSocketIoFrame("4", ns) is SocketIoIn.Ignored)
        assertTrue(parseSocketIoFrame("42/ws/ops,notjson", ns) is SocketIoIn.Ignored)
        assertTrue(parseSocketIoFrame("9zzz", ns) is SocketIoIn.Ignored)
    }
}
