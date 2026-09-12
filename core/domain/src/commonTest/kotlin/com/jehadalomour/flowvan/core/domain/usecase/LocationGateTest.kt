package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rule every document goes through.
 *
 * THREE independent facts decide it, and the third one was missing: whether the
 * OFFICE locked this rep, whether the PHONE grants this app location, and
 * whether location is switched on for the device at all. Granting the app
 * permission and then turning location off is the combination that used to pass
 * — a rep could sign in, sell all day, and the office believed the requirement
 * was being enforced the whole time.
 */
class LocationGateTest {

    private class FakeLocation(
        private val granted: Boolean,
        private val serviceOn: Boolean = true,
        private val fix: LatLng? = null,
    ) : LocationProvider {
        override suspend fun lastLocation(): LatLng? = fix
        override fun hasPermission(): Boolean = granted
        override fun isServiceEnabled(): Boolean = serviceOn
    }

    private fun session(locked: Boolean): SessionStore =
        SessionStore(MapSettings()).apply { requireLocation = locked }

    private fun gate(
        locked: Boolean,
        granted: Boolean,
        serviceOn: Boolean = true,
        fix: LatLng? = null,
    ): LocationGate = LocationGate(session(locked), FakeLocation(granted, serviceOn, fix))

    @Test
    fun `an unlocked rep writes whatever the phone says`() {
        assertTrue(gate(locked = false, granted = false).allowed())
        assertTrue(gate(locked = false, granted = true).allowed())
        // Including with location switched off entirely — this rep was never
        // asked for it, and a requirement nobody set must not appear from here.
        assertTrue(gate(locked = false, granted = false, serviceOn = false).allowed())
        assertEquals(LocationBlock.NONE, gate(locked = false, granted = false, serviceOn = false).check())
    }

    @Test
    fun `a locked rep with location on writes`() {
        assertTrue(gate(locked = true, granted = true).allowed())
        assertEquals(LocationBlock.NONE, gate(locked = true, granted = true).check())
    }

    @Test
    fun `a locked rep who denied the app is refused`() {
        val g = gate(locked = true, granted = false)
        assertFalse(g.allowed())
        assertEquals(LocationBlock.PERMISSION_DENIED, g.check())
        assertFailsWith<LocationRequiredException> { g.require() }
    }

    @Test
    fun `a locked rep who allowed the app but switched location off is refused`() {
        // The hole this closes. Permission says granted; no position will ever
        // be produced. Half a rule enforced is worse than none, because the
        // office thinks it has the whole one.
        val g = gate(locked = true, granted = true, serviceOn = false)
        assertFalse(g.allowed())
        assertEquals(LocationBlock.SERVICE_OFF, g.check())
        assertFailsWith<LocationRequiredException> { g.require() }
    }

    @Test
    fun `the refusal says which fault it was`() {
        // The screen sends the rep to a different settings page for each, so a
        // refusal that does not distinguish them is a refusal that cannot be acted on.
        val denied = assertFailsWith<LocationRequiredException> {
            gate(locked = true, granted = false).require()
        }
        assertEquals(LocationBlock.PERMISSION_DENIED, denied.block)

        val off = assertFailsWith<LocationRequiredException> {
            gate(locked = true, granted = true, serviceOn = false).require()
        }
        assertEquals(LocationBlock.SERVICE_OFF, off.block)
    }

    @Test
    fun `a denied permission is reported even when location is also off`() {
        // Both wrong at once. The permission is the one the rep can fix from the
        // app, so that is the one to name first; sending them to the device's
        // location page would leave them there with the app still denied.
        assertEquals(
            LocationBlock.PERMISSION_DENIED,
            gate(locked = true, granted = false, serviceOn = false).check(),
        )
    }

    @Test
    fun `settings decide it, not whether a fix arrived`() {
        // A rep who allowed location, has it switched on, and is standing inside
        // a warehouse has no fix yet. Refusing them there would be a bug the
        // office could not explain.
        val indoors = gate(locked = true, granted = true, serviceOn = true, fix = null)
        assertTrue(indoors.allowed())
        indoors.require()
    }
}

/**
 * What the DEVICE is refusing, asked separately from whether this rep must comply.
 *
 * The lock screen needs these apart. It decides WHETHER to lock from a fresh
 * answer off the server — never from the session cache, which cannot tell "the
 * office requires this" from "the office required it last time anyone asked",
 * and left a rep shut out of the whole app after the requirement had been
 * switched off. It then uses this only to say WHICH fault to explain.
 */
class LocationDeviceBlockTest {

    private class FakeLocation(
        private val granted: Boolean,
        private val serviceOn: Boolean,
    ) : LocationProvider {
        override suspend fun lastLocation(): LatLng? = null
        override fun hasPermission(): Boolean = granted
        override fun isServiceEnabled(): Boolean = serviceOn
    }

    private fun gate(granted: Boolean, serviceOn: Boolean, locked: Boolean = false) =
        LocationGate(
            SessionStore(MapSettings()).apply { requireLocation = locked },
            FakeLocation(granted, serviceOn),
        )

    @Test
    fun `reports nothing wrong when location is on`() {
        assertEquals(LocationBlock.NONE, gate(granted = true, serviceOn = true).deviceBlock())
    }

    @Test
    fun `reports a denied permission`() {
        assertEquals(
            LocationBlock.PERMISSION_DENIED,
            gate(granted = false, serviceOn = true).deviceBlock(),
        )
    }

    @Test
    fun `reports a switched-off service`() {
        assertEquals(
            LocationBlock.SERVICE_OFF,
            gate(granted = true, serviceOn = false).deviceBlock(),
        )
    }

    @Test
    fun `answers the same whatever the session says`() {
        // The whole point of it being separate: this one describes the PHONE.
        // Whether the rep has to do anything about it is the server's to say,
        // and reading it from the session here is what trapped people.
        assertEquals(
            gate(granted = false, serviceOn = true, locked = false).deviceBlock(),
            gate(granted = false, serviceOn = true, locked = true).deviceBlock(),
        )
    }
}
