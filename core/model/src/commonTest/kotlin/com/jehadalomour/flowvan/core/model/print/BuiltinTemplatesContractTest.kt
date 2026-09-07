package com.jehadalomour.flowvan.core.model.print

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The app against the layouts the API really ships — see [BuiltinTemplatesFixture].
 *
 * These assertions are the print-templates contract restated from the printing
 * side: every element type is one this renderer draws, every token is one the
 * resolver knows, and the fixed/flow split matches the zone each element sits
 * in. A backend change that breaks any of them fails here.
 */
class BuiltinTemplatesContractTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parsed(): PrintTemplatesResponse =
        json.decodeFromString(PrintTemplatesResponse.serializer(), BuiltinTemplatesFixture.JSON)

    private val ctx = TemplateContext(
        companyNameAr = "مشروبات الأردن",
        companyTaxNumber = "12345678",
        companyPhone1 = "+962 6 500 0000",
        companyAddressAr = "Amman - Jordan",
        invoiceNumber = "TRF-MAIN000001",
        invoiceKind = "TRANSFER",
        invoiceDate = "2026-09-07",
        invoiceTime = "12:15",
        cashier = "admin",
        customerName = "سوبرماركت السلام",
        fromStore = "Main Warehouse",
        toStore = "Van 1 - Amman",
        reference = "INV-VAN1000001",
        note = "ملاحظة",
        paymentType = "CASH",
        payments = "CASH: 6.500 د.أ",
        subtotal = 5.7,
        discount = 0.1,
        taxTotal = 0.896,
        total = 6.496,
        paid = 6.5,
        currency = "د.أ",
        decimals = 3,
        lines = listOf(
            TemplateLine(name = "كوكا كولا ٣٣٠مل", sku = "C330", qty = 6.0, unit = "حبة", price = 0.45, taxRate = 0.16, gross = 2.7, total = 3.132),
        ),
    )

    @Test
    fun the_shipped_layouts_parse() {
        val r = parsed()
        assertEquals(setOf("SALE", "TRANSFER", "PAYMENT_IN"), r.templates.keys)
        r.templates.forEach { (kind, t) ->
            assertEquals(kind, t.documentType, "documentType of $kind")
            assertEquals(PaperSize.THERMAL_80, t.paperSize, "$kind prints on the field printers' paper")
            assertEquals(80.0, t.layout.layout.width, "$kind page width")
            assertTrue(t.layout.elements.isNotEmpty(), "$kind has elements")
        }
    }

    @Test
    fun every_element_is_one_this_renderer_draws() {
        val unsupported = parsed().templates.values
            .flatMap { it.layout.elements }
            .filterNot { it.isSupported }
            .map { it.type }
            .toSet()
        assertEquals(emptySet(), unsupported, "element types this app cannot draw")
    }

    @Test
    fun fixed_elements_stay_out_of_the_body() {
        // A zone's height is a minimum and a fixed element does not push a flow
        // element down; the contract therefore keeps tables in the body and
        // everything positioned in the header and footer.
        val misplaced = parsed().templates.values
            .flatMap { it.layout.elements }
            .filter { it.isFlow != (it.zone == Zone.BODY) }
            .map { "${it.type}@${it.zone}" }
        assertEquals(emptyList(), misplaced)
    }

    @Test
    fun every_token_the_layouts_use_resolves() {
        val tokenRe = Regex("""\{\{([^}]+)}}""")
        val unresolved = mutableSetOf<String>()
        for (t in parsed().templates.values) {
            val texts = t.layout.elements.flatMap { e ->
                listOfNotNull(e.props.content, e.props.data) + (e.props.rows ?: emptyList()).map { it.value }
            }
            for (text in texts) {
                for (m in tokenRe.findAll(text)) {
                    val token = m.groupValues[1].trim()
                    // An empty answer is legitimate (a blank field); an unknown
                    // token is not, and the resolver returns the raw text for it.
                    if (TemplateTokens.resolve("{{$token}}", ctx).contains("{{")) unresolved += token
                }
            }
        }
        assertEquals(emptySet(), unresolved, "tokens the resolver does not know")
    }

    @Test
    fun the_transfer_voucher_prints_its_stores_and_number() {
        val transfer = assertNotNull(parsed().templates["TRANSFER"])
        val printed = transfer.layout.elements
            .mapNotNull { it.props.content }
            .joinToString("\n") { TemplateTokens.resolve(it, ctx) }
        assertTrue(printed.contains("TRF-MAIN000001"), "voucher number")
        assertTrue(printed.contains("Main Warehouse"), "from store")
        assertTrue(printed.contains("Van 1 - Amman"), "to store")
        assertTrue(printed.contains("مشروبات الأردن"), "company name")
    }

    @Test
    fun the_sale_totals_block_foots_the_way_the_dashboard_prints_it() {
        val sale = assertNotNull(parsed().templates["SALE"])
        val totals = sale.layout.elements.first { it.type == ElementType.TOTALS_BLOCK }
        val values = (totals.props.rows ?: emptyList()).associate { it.label to TemplateTokens.resolve(it.value, ctx) }
        assertEquals("5.700 د.أ", values["Total"])
        assertEquals("0.100 د.أ", values["Total discount"])
        assertEquals("0.896 د.أ", values["Total tax"])
        assertEquals("6.496 د.أ", values["Net total"])
    }
}
