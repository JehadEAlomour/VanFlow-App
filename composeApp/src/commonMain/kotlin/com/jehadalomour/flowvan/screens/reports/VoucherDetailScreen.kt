package com.jehadalomour.flowvan.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import com.jehadalomour.flowvan.shared.presentation.feature.voucherdetail.VoucherDetailViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VoucherDetailScreen(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: VoucherDetailViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsState()
    val entity = state.entity

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text(
                    when (entity?.type) {
                        "SALE" -> "فاتورة مبيعات"
                        "RETURN" -> "فاتورة مرتجع"
                        "REQUEST" -> "طلب توريد"
                        else -> "سند"
                    },
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fv.Blue)
                }
                entity == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لم يتم العثور على السند", color = Fv.TextMid)
                }
                else -> VoucherContent(entity, state.lines)
            }
        }
    }
}

@Composable
private fun VoucherContent(entity: InvoiceEntity, lines: List<InvoiceLine>) {
    val (typeLabel, typeColor) = when (entity.type) {
        "SALE" -> "بيع" to Fv.Green
        "RETURN" -> "مرتجع" to Fv.Red
        "REQUEST" -> "طلب" to Fv.Teal
        else -> entity.type to Fv.TextMid
    }
    val (statusLabel, statusColor) = when (entity.status) {
        "CONFIRMED" -> "مؤكدة" to Fv.Green
        "CANCELLED" -> "ملغاة" to Fv.Red
        "FULFILLED" -> "منفذة" to Fv.Blue
        else -> "مسودة" to Fv.Amber
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Fv.Surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorBadge(typeLabel, typeColor)
                        Spacer(Modifier.width(8.dp))
                        ColorBadge(statusLabel, statusColor, alpha = 0.12f)
                        Spacer(Modifier.weight(1f))
                        Text(entity.number, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Fv.SurfaceHigh)
                    LabelValueRow("التاريخ", entity.createdAt.toDateTimeString())
                    entity.paymentMethod?.let { LabelValueRow("طريقة الدفع", paymentMethodLabel(it)) }
                    entity.notes?.takeIf { it.isNotBlank() }?.let { LabelValueRow("ملاحظات", it) }
                }
            }
        }

        if (lines.isNotEmpty()) {
            item {
                Text("البنود", color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            items(lines) { line -> LineRow(line) }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Fv.Surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الإجماليات", color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(color = Fv.SurfaceHigh)
                    TotalRow("المجموع الفرعي", entity.subtotal.formatJod(AppLanguage.AR))
                    if (entity.discountAmount > 0)
                        TotalRow("الخصم", "-${entity.discountAmount.formatJod(AppLanguage.AR)}", Fv.Red)
                    if (entity.taxAmount > 0)
                        TotalRow("الضريبة", entity.taxAmount.formatJod(AppLanguage.AR))
                    HorizontalDivider(color = Fv.SurfaceHigh)
                    TotalRow("الإجمالي", entity.total.formatJod(AppLanguage.AR), Fv.TextHigh, isBold = true)
                }
            }
        }
    }
}

@Composable
private fun LineRow(line: InvoiceLine) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(line.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(line.sku, color = Fv.TextMid, fontSize = 10.sp)
                }
                Text(line.lineTotal.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStat("الكمية", if (line.qty == line.qty.toLong().toDouble()) line.qty.toLong().toString() else "%.2f".format(line.qty))
                SmallStat("سعر الوحدة", line.unitPrice.formatJod(AppLanguage.AR))
                if (line.discountPct > 0) SmallStat("خصم", "%.0f%%".format(line.discountPct))
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String) {
    Column {
        Text(label, color = Fv.TextMid, fontSize = 9.sp)
        Text(value, color = Fv.TextHigh, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TotalRow(label: String, value: String, color: Color = Fv.TextMid, isBold: Boolean = false) {
    Row {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = if (isBold) 14.sp else 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
internal fun ColorBadge(label: String, color: Color, alpha: Float = 0.18f) {
    Box(
        modifier = Modifier.background(color.copy(alpha = alpha), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

private fun paymentMethodLabel(method: String) = when (method) {
    "CASH" -> "نقداً"
    "CHEQUE" -> "شيك"
    "TRANSFER" -> "تحويل"
    "CREDIT" -> "آجل"
    else -> method
}
