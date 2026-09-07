package com.jehadalomour.flowvan.core.model.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec §3: which element types are placed absolutely and which stack in the flow. */
class ElementClassificationTest {

    private fun of(type: String) = Element(id = type.lowercase(), type = type)

    @Test
    fun fixed_types_are_placed_absolutely() {
        listOf("LOGO", "TEXT", "DIVIDER", "SPACER", "QR_CODE").forEach { type ->
            val e = of(type)
            assertFalse(e.isFlow, "$type must be fixed")
            assertTrue(e.isSupported, "$type must be supported")
        }
    }

    @Test
    fun flow_types_stack_in_element_order() {
        listOf("ITEMS_TABLE", "TOTALS_BLOCK", "TAX_INVOICE", "REPORT_TABLE", "SHIFTS_TABLE").forEach { type ->
            val e = of(type)
            assertTrue(e.isFlow, "$type must flow")
            assertTrue(e.isSupported, "$type must be supported")
        }
    }

    @Test
    fun an_unknown_type_is_neither_and_is_skipped() {
        val e = of("HOLOGRAM")
        assertFalse(e.isFlow)
        assertFalse(e.isSupported)
    }

    @Test
    fun fixture_body_has_only_flow_and_header_only_fixed() {
        val r = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(PrintTemplatesResponse.serializer(), SaleThermalFixture.json)
        val sale = r.templates.getValue("SALE")
        assertTrue(sale.layout.elementsIn(Zone.BODY).all { it.isFlow })
        assertTrue(sale.layout.elementsIn(Zone.HEADER).none { it.isFlow })
        // The footer mixes fixed elements only; a flow element could sit there too.
        assertEquals(listOf(false, false, false), sale.layout.elementsIn(Zone.FOOTER).map { it.isFlow })
    }
}
