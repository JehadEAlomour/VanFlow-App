package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.voucher_logo
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.PaymentType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.painterResource

// A clean A4 invoice palette (mirrors the ERP web print: light borders, gray header).
private val Ink = Color(0xFF111111)
private val Muted = Color(0xFF666666)
private val Faint = Color(0xFF999999)
private val Line = Color(0xFFE0E0E0)
private val HeadBg = Color(0xFFF4F4F4)
private val AccentBlue = Color(0xFF1565C0)
private val PaidGreen = Color(0xFF2E7D32)
private val DueRed = Color(0xFFC62828)

/**
 * A4 invoice document for a voucher — modelled on the ERP's web print: an LTR page with
 * an INVOICE title, a BILL TO block, a bordered item table (SKU / Description / Qty / Unit
 * / Unit Price / Tax / Discount / Total, "—" where empty) and a right-aligned totals block
 * (Subtotal, Discount, Tax, Total, Paid, Outstanding). Enhanced with the company logo and
 * phone in the header. Arabic names render correctly inline within the LTR layout.
 *
 * This is the on-screen voucher view AND the "Share as PDF" source.
 */
@Composable
fun VoucherA4Document(state: VoucherPrintState) {
    val decimals = state.template.amountDecimals
    val sym = state.template.currency
    val showTax = !state.isTaxExempt
    val showDiscount = state.canPrintLineDiscount

    val paymentType = PaymentType.fromPaymentMethod(state.paymentMethod)
    val isCredit = paymentType == PaymentType.CREDIT
    val paid = if (isCredit) 0.0 else state.total
    val outstanding = state.total - paid

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {

        // ── Top strip: date · document number ───────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.createdAt.a4DateTime(), fontSize = 9.sp, color = Faint)
            Text(state.number, fontSize = 9.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))

        // ── Header: logo + company (left)  ·  title (right) ─────────────────
        val companyName = state.companyNameAr.ifBlank { state.companyNameEn }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
                if (logo != null) {
                    Image(bitmap = logo, contentDescription = null, modifier = Modifier.heightIn(max = 64.dp))
                } else {
                    Image(painter = painterResource(Res.drawable.voucher_logo), contentDescription = null, modifier = Modifier.height(56.dp))
                }
                Spacer(Modifier.height(6.dp))
                if (companyName.isNotBlank()) {
                    Text(companyName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                if (state.companyPhone.isNotBlank()) {
                    Text(state.companyPhone, fontSize = 12.sp, color = Muted)
                }
                if (state.companyTaxNumber.isNotBlank()) {
                    Text("Tax No: ${state.companyTaxNumber}", fontSize = 11.sp, color = Muted)
                }
            }
            Text(
                text = state.type.docTitle(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── BILL TO ─────────────────────────────────────────────────────────
        Text("BILL TO", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
        Text(state.customerNameAr, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        if (state.customerCode.isNotBlank()) Text(state.customerCode, fontSize = 11.sp, color = Muted)
        state.customerTaxNumber?.takeIf { it.isNotBlank() }?.let { Text("Tax No: $it", fontSize = 11.sp, color = Muted) }

        Spacer(Modifier.height(12.dp))

        // ── Meta: document no / date / salesman / payment ───────────────────
        MetaRow("Document No.", state.number)
        MetaRow("Date", state.createdAt.a4DateOnly())
        if (state.salesmanNameAr.isNotBlank()) MetaRow("Salesman", state.salesmanNameAr)
        MetaRow("Payment", paymentType.labelEn)

        Spacer(Modifier.height(14.dp))

        // ── Item table ──────────────────────────────────────────────────────
        ItemTable(state.lines, state.freeLines, showTax, showDiscount, decimals, sym)

        Spacer(Modifier.height(14.dp))

        // ── Totals (right half) ─────────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Column(Modifier.weight(1.2f)) {
                TotalRow("Subtotal", a4Money(state.subtotal, decimals, sym))
                if (state.discountAmount > 0.0) {
                    TotalRow("Discount", "-" + a4Money(state.discountAmount, decimals, sym))
                }
                if (showTax) {
                    TotalRow("Tax", a4Money(state.taxAmount, decimals, sym))
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
                Spacer(Modifier.height(4.dp))
                TotalRow("Total", a4Money(state.total, decimals, sym), bold = true, big = true)
                TotalRow("Paid", a4Money(paid, decimals, sym), valueColor = PaidGreen)
                TotalRow("Outstanding", a4Money(outstanding, decimals, sym), bold = true, valueColor = DueRed)
            }
        }

        if (state.isTaxExempt) {
            Spacer(Modifier.height(8.dp))
            Text("Tax-exempt invoice", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        }

        // ── Notes ───────────────────────────────────────────────────────────
        Spacer(Modifier.height(18.dp))
        Text("NOTES", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
        Text(state.notes?.takeIf { it.isNotBlank() } ?: "—", fontSize = 11.sp, color = Muted)

        // ── Signatures ──────────────────────────────────────────────────────
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            SignatureCell("Prepared By", Modifier.weight(1f))
            SignatureCell("Received By", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ItemTable(
    lines: List<InvoiceLine>,
    freeLines: List<InvoiceLine>,
    showTax: Boolean,
    showDiscount: Boolean,
    decimals: Int,
    sym: String,
) {
    Column(Modifier.fillMaxWidth()) {
        // Header
        Row(Modifier.fillMaxWidth().background(HeadBg)) {
            HeadCell("SKU", 1.6f, TextAlign.Start)
            HeadCell("Description", 2.6f, TextAlign.Start)
            HeadCell("Qty", 1f, TextAlign.End)
            HeadCell("Unit", 1f, TextAlign.Start)
            HeadCell("Unit Price", 1.4f, TextAlign.End)
            if (showTax) HeadCell("Tax", 0.9f, TextAlign.End)
            if (showDiscount) HeadCell("Discount", 1.2f, TextAlign.End)
            HeadCell("Total", 1.4f, TextAlign.End)
        }
        lines.forEach { ItemRow(it, showTax, showDiscount, decimals, sym, gift = false) }
        freeLines.forEach { ItemRow(it, showTax, showDiscount, decimals, sym, gift = true) }
    }
}

@Composable
private fun ItemRow(
    line: InvoiceLine,
    showTax: Boolean,
    showDiscount: Boolean,
    decimals: Int,
    sym: String,
    gift: Boolean,
) {
    val gross = line.qty * line.unitPrice
    val price = if (gift) 0.0 else line.unitPrice
    val discountVal = if (gift) gross else gross * line.discountPct
    val total = if (gift) 0.0 else gross
    val name = if (gift) "${line.nameAr} (Gift)" else line.nameAr
    val taxCell = if (line.taxRate > 0.0) a4Rate(line.taxRate) else "—"
    val discCell = if (discountVal > 0.0) a4Money(discountVal, decimals, sym) else "—"

    Row(Modifier.fillMaxWidth()) {
        BodyCell(line.sku, 1.6f, TextAlign.Start)
        BodyCell(name, 2.6f, TextAlign.Start)
        BodyCell(formatQty(line.qty), 1f, TextAlign.End)
        BodyCell(line.unit.ifBlank { "—" }, 1f, TextAlign.Start)
        BodyCell(a4Money(price, decimals, sym), 1.4f, TextAlign.End)
        if (showTax) BodyCell(taxCell, 0.9f, TextAlign.End)
        if (showDiscount) BodyCell(discCell, 1.2f, TextAlign.End)
        BodyCell(a4Money(total, decimals, sym), 1.4f, TextAlign.End, bold = true)
    }
}

// ── Building blocks ─────────────────────────────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeadCell(text: String, weight: Float, align: TextAlign) {
    Box(
        Modifier.weight(weight)
            .background(HeadBg)
            .border(0.7.dp, Line)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = align, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    align: TextAlign,
    bold: Boolean = false,
) {
    Box(
        Modifier.weight(weight)
            .border(0.7.dp, Line)
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 11.sp, color = Ink, textAlign = align, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, fontSize = 11.sp, color = Muted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 11.sp, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    bold: Boolean = false,
    big: Boolean = false,
    valueColor: Color = Ink,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = if (bold) Ink else Muted, fontSize = if (big) 15.sp else 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = valueColor, fontSize = if (big) 15.sp else 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun SignatureCell(label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

// ── Formatting (Latin digits, locale-independent — mirrors the receipt) ──────────

private fun a4Amount(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val whole = scaled / factor
    val frac = scaled % factor
    val sb = StringBuilder()
    if (value < 0) sb.append('-')
    sb.append(whole.toString())
    if (decimals > 0) sb.append('.').append(frac.toString().padStart(decimals, '0'))
    return sb.toString()
}

private fun a4Money(value: Double, decimals: Int, currency: String): String =
    "${a4Amount(value, decimals)} $currency"

private fun a4Rate(rate: Double): String =
    if (rate == rate.toLong().toDouble()) "${rate.toLong()}%" else "${a4Amount(rate, 2)}%"

private fun String.docTitle(): String = when (uppercase()) {
    "SALE" -> "INVOICE"
    "RETURN" -> "RETURN"
    "ORDER" -> "ORDER"
    else -> uppercase()
}

private fun Long.a4DateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val p = { n: Int -> n.toString().padStart(2, '0') }
    return "${dt.year}-${p(dt.monthNumber)}-${p(dt.dayOfMonth)} ${p(dt.hour)}:${p(dt.minute)}"
}

private fun Long.a4DateOnly(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val p = { n: Int -> n.toString().padStart(2, '0') }
    return "${dt.year}-${p(dt.monthNumber)}-${p(dt.dayOfMonth)}"
}
