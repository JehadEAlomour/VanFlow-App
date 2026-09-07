package com.jehadalomour.flowvan.core.model.print

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateTokensTest {

    private val ctx = TemplateContext(
        companyNameAr = "شركة النور",
        companyNameEn = "Al Noor Co.",
        companyTaxNumber = "123456789",
        invoiceNumber = "S-000123",
        invoiceKind = "SALE",
        invoiceDate = "2026-09-07",
        invoiceTime = "14:05",
        cashier = "أحمد",
        customerName = "بقالة السلام",
        customerNumber = "C-77",
        subtotal = 12.5,
        discount = 0.5,
        taxTotal = 1.92,
        total = 13.92,
        paid = 13.92,
        change = 0.0,
        payments = "CASH: 13.920 JOD",
        paymentType = "نقدي",
        isTaxExempt = false,
        taxExemptStamp = "فاتورة معفاة من الضريبة",
        qrData = "QR-PAYLOAD",
        lines = listOf(
            TemplateLine(name = "ماء 1.5 لتر", sku = "W15", qty = 6.0, unit = "حبة", price = 0.5, taxRate = 0.16, discount = 0.0, tax = 0.48, gross = 3.0, total = 3.48),
            TemplateLine(name = "عصير", sku = "J1", qty = 1.0, unit = "كرتونة", price = 2.0, taxRate = 0.0, discount = 2.0, tax = 0.0, gross = 2.0, total = 0.0, isGift = true),
        ),
        decimals = 3,
        currency = "JOD",
    )

    @Test
    fun money_tokens_use_company_decimals_and_currency() {
        assertEquals("12.500 JOD", TemplateTokens.resolve("{{invoice.subtotal}}", ctx))
        assertEquals("13.920 JOD", TemplateTokens.resolve("{{invoice.total}}", ctx))
        assertEquals("0.000 JOD", TemplateTokens.resolve("{{invoice.change}}", ctx))
        assertEquals("1.92 JOD", TemplateTokens.resolve("{{invoice.taxTotal}}", ctx.copy(decimals = 2)))
        assertEquals("13.920", TemplateTokens.resolve("{{invoice.total}}", ctx.copy(currency = "")))
    }

    @Test
    fun amounts_are_latin_digits_with_rounding_and_sign() {
        assertEquals("0.005", TemplateTokens.formatAmount(0.0049, 3))
        assertEquals("-1.250", TemplateTokens.formatAmount(-1.25, 3))
        assertEquals("0.000", TemplateTokens.formatAmount(-0.0001, 3))
        assertEquals("7", TemplateTokens.formatAmount(7.4, 0))
        assertEquals("6", TemplateTokens.formatQty(6.0))
        assertEquals("2.50", TemplateTokens.formatQty(2.5))
    }

    @Test
    fun unknown_token_renders_empty_and_text_around_it_survives() {
        assertEquals("A  B", TemplateTokens.resolve("A {{invoice.nothingHere}} B", ctx))
        // Braces with no token name are not a placeholder and stay as written.
        assertEquals("{{ }}", TemplateTokens.resolve("{{ }}", ctx))
        assertEquals("", TemplateTokens.resolve(null, ctx))
        assertEquals("plain", TemplateTokens.resolve("plain", ctx))
    }

    @Test
    fun kind_name_is_the_arabic_name_from_the_spec() {
        assertEquals("سند بيع", TemplateTokens.resolve("{{invoice.kindName}}", ctx))
        assertEquals("سند مرتجع", TemplateTokens.resolve("{{invoice.kindName}}", ctx.copy(invoiceKind = "RETURN")))
        assertEquals("طلبية", TemplateTokens.resolve("{{invoice.kindName}}", ctx.copy(invoiceKind = "ORDER")))
        assertEquals("سند قبض", VoucherKinds.arabicName("PAYMENT_IN"))
        assertEquals("تحميل المركبة", VoucherKinds.arabicName("TRANSFER_IN"))
        assertEquals("SALE", TemplateTokens.resolve("{{invoice.kind}}", ctx))
        // The app's REQUEST voucher is the designer's ORDER.
        assertEquals("ORDER", VoucherKinds.documentTypeFor("REQUEST"))
        assertEquals("SALE", VoucherKinds.documentTypeFor("SALE"))
    }

    @Test
    fun multiple_tokens_whitespace_and_newlines_resolve_in_one_string() {
        val out = TemplateTokens.resolve(
            "رقم: {{invoice.number}}\nالتاريخ: {{ invoice.date }} {{invoice.time}}\n{{company.nameAr}} / {{company.nameEn}}",
            ctx,
        )
        assertEquals("رقم: S-000123\nالتاريخ: 2026-09-07 14:05\nشركة النور / Al Noor Co.", out)
    }

    @Test
    fun tax_exempt_stamp_prints_only_when_exempt() {
        assertEquals("", TemplateTokens.resolve("{{invoice.taxExempt}}", ctx))
        assertEquals(
            "فاتورة معفاة من الضريبة",
            TemplateTokens.resolve("{{invoice.taxExempt}}", ctx.copy(isTaxExempt = true)),
        )
    }

    @Test
    fun item_count_payments_and_qr_tokens() {
        assertEquals("2", TemplateTokens.resolve("{{invoice.itemCount}}", ctx))
        assertEquals("CASH: 13.920 JOD", TemplateTokens.resolve("{{invoice.payments}}", ctx))
        assertEquals("نقدي", TemplateTokens.resolve("{{invoice.paymentType}}", ctx))
        assertEquals("QR-PAYLOAD", TemplateTokens.resolve("{{invoice.qrData}}", ctx))
    }

    @Test
    fun gift_line_name_gets_the_arabic_suffix() {
        val gift = ctx.lines[1]
        assertEquals("عصير (هدية)", TemplateTokens.cell("name", gift, ctx))
        assertEquals("ماء 1.5 لتر", TemplateTokens.cell("name", ctx.lines[0], ctx))
    }

    @Test
    fun table_cells_by_column_key() {
        val line = ctx.lines[0]
        assertEquals("W15", TemplateTokens.cell("sku", line, ctx))
        assertEquals("6", TemplateTokens.cell("qty", line, ctx))
        assertEquals("حبة", TemplateTokens.cell("unit", line, ctx))
        assertEquals("0.500", TemplateTokens.cell("price", line, ctx))
        assertEquals("16%", TemplateTokens.cell("taxPct", line, ctx))
        assertEquals("0.000", TemplateTokens.cell("discount", line, ctx))
        assertEquals("0.480", TemplateTokens.cell("tax", line, ctx))
        assertEquals("3.480", TemplateTokens.cell("total", line, ctx))
        assertEquals("3.000", TemplateTokens.cell("gross", line, ctx))
        assertEquals("", TemplateTokens.cell("taxPct", ctx.lines[1], ctx))
        assertEquals("", TemplateTokens.cell("unknownColumn", line, ctx))
    }
}
