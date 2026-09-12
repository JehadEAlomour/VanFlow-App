package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rule every document goes through. Two independent facts decide it: whether
 * the OFFICE locked this rep, and whether the PHONE grants location.
 */
class LocationGateTest {

    private class FakeLocation(
        private val granted: Boolean,
        private val fix: LatLng? = null,
    ) : LocationProvider {
        override suspend fun lastLocation(): LatLng? = fix
        override fun hasPermission(): Boolean = granted
    }

    private fun session(locked: Boolean): SessionStore =
        SessionStore(MapSettings()).apply { requireLocation = locked }

    private fun gate(locked: Boolean, granted: Boolean, fix: LatLng? = null): LocationGate =
        LocationGate(session(locked), FakeLocation(granted, fix))

    @Test
    fun `an unlocked rep writes whatever the phone says`() {
        assertTrue(gate(locked = false, granted = false).allowed())
        assertTrue(gate(locked = false, granted = true).allowed())
    }

    @Test
    fun `a locked rep with location on writes`() {
        assertTrue(gate(locked = true, granted = true).allowed())
    }

    @Test
    fun `a locked rep with location off is refused`() {
        assertFalse(gate(locked = true, granted = false).allowed())
        assertFailsWith<LocationRequiredException> { gate(locked = true, granted = false).require() }
    }

    @Test
    fun `permission decides it, not whether a fix arrived`() {
        // A rep who allowed location and is standing inside a warehouse has no fix
        // yet. Refusing them there would be a bug the office could not explain.
        val indoors = gate(locked = true, granted = true, fix = null)
        assertTrue(indoors.allowed())
        indoors.require()
    }
}
