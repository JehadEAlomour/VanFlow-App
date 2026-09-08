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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.voucher_logo
import org.jetbrains.compose.resources.painterResource

private val Ink = Color(0xFF111111)
private val Muted = Color(0xFF666666)
private val Faint = Color(0xFF999999)
private val AccentBlue = Color(0xFF1565C0)

/**
 * A4 page for a collection receipt (سند قبض) — the shareable counterpart of the thermal
 * slip, for a customer who wants the receipt by email or WhatsApp rather than on paper.
 *
 * Deliberately the same stationery as [VoucherA4Document]: same palette, same header
 * block, same meta rows and signature line, so an invoice and its receipt look like they
 * came from one company. What differs is the body — a receipt has no item table and no
 * tax. It states one figure, and the whole page is built around making that figure and
 * what settles it unmistakable, because that is the entire content of the document.
 *
 * Money only: a payment carries no tax of its own (the tax was charged on the invoice),
 * so nothing here computes or prints a tax line.
 */
@Composable
fun ReceiptA4Document(entity: PaymentEntity, state: ReceiptDetailState) {
    val methodEn = when (entity.method) {
        "CASH" -> "Cash"
        "CHEQUE" -> "Cheque"
        "TRANSFER" -> "Bank Transfer"
        else -> entity.method
    }
    val methodAr = when (entity.method) {
        "CASH" -> "نقدي"
        "CHEQUE" -> "شيك"
        "TRANSFER" -> "حوالة"
        else -> entity.method
    }

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {

        // ── Top strip: date · document number ───────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entity.createdAt.a4DateTime(), fontSize = 9.sp, color = Faint)
            Text(entity.number, fontSize = 9.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))

        // ── Header: logo + company (left) · title (right) ────────────────────
        val companyName = state.companyNameAr.ifBlank { state.companyNameEn.orEmpty() }
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
            Column(horizontalAlignment = Alignment.End) {
                Text("RECEIPT", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("سند قبض", fontSize = 14.sp, color = Muted)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── RECEIVED FROM ───────────────────────────────────────────────────
        // The invoice says BILL TO; a receipt is the other direction, and naming it
        // that way is what stops the two documents being mistaken for each other.
        Text("RECEIVED FROM", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
        Text(
            state.customerNameAr.ifBlank { state.customerCode },
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
        )
        if (state.customerCode.isNotBlank()) Text(state.customerCode, fontSize = 11.sp, color = Muted)

        Spacer(Modifier.height(12.dp))

        // ── Meta ────────────────────────────────────────────────────────────
        MetaRow("Receipt No.", entity.number)
        MetaRow("Date", entity.createdAt.a4DateOnly())
        if (state.salesmanNameAr.isNotBlank()) MetaRow("Salesman", state.salesmanNameAr)
        MetaRow("Method", "$methodEn / $methodAr")

        Spacer(Modifier.height(18.dp))

        // ── The amount ──────────────────────────────────────────────────────
        // A receipt is one number. It gets a box of its own rather than a totals
        // column, because there is nothing for it to be totalled against.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Ink)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("AMOUNT RECEIVED", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
                    Text("المبلغ المستلم", fontSize = 11.sp, color = Muted)
                }
                Text(
                    a4Money(entity.amount, RECEIPT_DECIMALS, RECEIPT_CURRENCY),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
        }

        // ── Cheque / transfer particulars ───────────────────────────────────
        // What makes the payment traceable. A cheque receipt without the bank and
        // number is not evidence of anything, so these ride the page whenever present.
        if (entity.method == "CHEQUE") {
            Spacer(Modifier.height(16.dp))
            Text("CHEQUE DETAILS", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            entity.chequeBank?.takeIf { it.isNotBlank() }?.let { MetaRow("Bank", it) }
            entity.chequeNumber?.takeIf { it.isNotBlank() }?.let { MetaRow("Cheque No.", it) }
            entity.chequeDate?.let { MetaRow("Due Date", it.a4DateOnly()) }
        }

        val transferRef = entity.transferRef
        if (entity.method == "TRANSFER" && !transferRef.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text("TRANSFER DETAILS", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            MetaRow("Reference", transferRef)
        }

        // ── Notes ───────────────────────────────────────────────────────────
        Spacer(Modifier.height(18.dp))
        Text("NOTES", fontSize = 10.sp, color = Faint, fontWeight = FontWeight.SemiBold)
        Text(entity.notes?.takeIf { it.isNotBlank() } ?: "—", fontSize = 11.sp, color = Muted)

        // ── Signatures ──────────────────────────────────────────────────────
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            SignatureCell("Received By", Modifier.weight(1f))
            SignatureCell("Customer", Modifier.weight(1f))
        }
    }
}

/** JOD, 3 decimals — the receipt has no invoice template to take these from. */
private const val RECEIPT_DECIMALS = 3
private const val RECEIPT_CURRENCY = "JOD"
