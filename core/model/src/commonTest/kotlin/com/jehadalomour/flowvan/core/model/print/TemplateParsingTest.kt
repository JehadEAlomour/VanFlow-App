package com.jehadalomour.flowvan.core.model.print

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parse(): PrintTemplatesResponse =
        json.decodeFromString(PrintTemplatesResponse.serializer(), SaleThermalFixture.json)

    @Test
    fun resolve_all_fixture_parses_every_kind() {
        val r = parse()
        assertEquals("2026-09-07T10:00:00.000Z", r.version)
        assertEquals(setOf("SALE", "RETURN"), r.templates.keys)
        val sale = assertNotNull(r.templates["SALE"])
        assertEquals("tpl_sale_van", sale.id)
        assertTrue(sale.isDefault)
        assertTrue(sale.isThermal)
        assertNull(sale.branchId)
        assertNull(r.templates["RETURN"]!!.id)
    }

    @Test
    fun page_layout_margins_and_zones_are_read() {
        val sale = parse().templates.getValue("SALE")
        val page = sale.layout.layout
        assertEquals(80.0, page.width)
        assertNull(page.height)
        assertEquals("mm", page.unit)
        assertEquals(3.0, page.margins.top)
        assertEquals(4.0, page.margins.left)
        assertEquals(38.0, page.zones.header.minHeight)
        assertEquals(30.0, page.zones.footer.minHeight)
        assertTrue(page.zones.body.flex)
        assertEquals(0.0, page.zones.body.minHeight)
    }

    @Test
    fun element_props_are_read_and_unknown_props_ignored() {
        val sale = parse().templates.getValue("SALE")
        val els = sale.layout.elements.associateBy { it.id }
        assertEquals(10, els.size)

        val name = els.getValue("name")
        assertEquals(ElementType.TEXT, name.type)
        assertEquals(Zone.HEADER, name.zone)
        assertEquals(13.0, name.y)
        assertEquals(8.0, name.height)
        assertEquals("{{company.nameAr}}", name.props.content)
        assertEquals(14.0, name.props.fontSizePt)
        assertTrue(name.props.isBold)
        assertEquals("rtl", name.props.direction)
        assertEquals("arabic", name.props.fontFamily)

        // Defaults kick in where the JSON is silent.
        val kind = els.getValue("kind")
        assertFalse(kind.props.isBold)
        assertEquals(ElementProps.DEFAULT_LINE_HEIGHT, kind.props.lineHeightFactor)
        assertEquals(1.3, els.getValue("meta").props.lineHeightFactor)

        // An unknown prop ("someFutureProp") and colour parse without error.
        assertEquals("#637181", els.getValue("thanks").props.color)

        val items = els.getValue("items")
        assertNull(items.height)
        val cols = assertNotNull(items.props.columns)
        assertEquals(listOf("name", "qty", "price", "taxPct", "total"), cols.map { it.key })
        assertTrue(cols[3].hide)
        assertEquals(3.0, cols[0].width)
        assertEquals("الصنف", cols[0].labelAr)

        val rows = assertNotNull(els.getValue("totals").props.rows)
        assertEquals(5, rows.size)
        assertTrue(rows.last().isBold)
        assertTrue(rows[3].hide)
        assertEquals("{{invoice.total}}", rows.last().value)

        assertEquals("dashed", els.getValue("rule1").props.style)
        assertEquals("{{invoice.qrData}}", els.getValue("qr").props.data)
        assertEquals("contain", els.getValue("logo").props.fit)
    }

    @Test
    fun elements_in_zone_keep_element_order() {
        val sale = parse().templates.getValue("SALE")
        assertEquals(listOf("logo", "name", "kind", "meta", "rule1"), sale.layout.elementsIn(Zone.HEADER).map { it.id })
        assertEquals(listOf("items", "totals"), sale.layout.elementsIn(Zone.BODY).map { it.id })
        assertEquals(listOf("qr", "thanks", "sp"), sale.layout.elementsIn(Zone.FOOTER).map { it.id })
    }

    @Test
    fun a_minimal_template_parses_with_defaults() {
        val t = json.decodeFromString(Template.serializer(), """{"documentType":"SALE"}""")
        assertEquals(PaperSize.THERMAL_80, t.paperSize)
        assertTrue(t.isThermal)
        assertEquals(80.0, t.layout.layout.width)
        assertTrue(t.layout.elements.isEmpty())
        val e = json.decodeFromString(Element.serializer(), """{"type":"TEXT"}""")
        assertEquals(Zone.BODY, e.zone)
        assertEquals(ElementProps.DEFAULT_FONT_SIZE_PT, e.props.fontSizePt)
    }

    @Test
    fun paper_widths_follow_the_spec() {
        assertEquals(210.0, PaperSize.widthMm(PaperSize.A4))
        assertEquals(148.0, PaperSize.widthMm(PaperSize.A5))
        assertEquals(80.0, PaperSize.widthMm(PaperSize.THERMAL_80))
        assertEquals(80.0, PaperSize.widthMm("SOMETHING_NEW"))
    }
}
