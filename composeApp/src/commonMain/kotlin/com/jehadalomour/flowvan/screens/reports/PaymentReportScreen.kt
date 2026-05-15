package com.jehadalomour.flowvan.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.presentation.feature.paymentreport.PaymentMethodFilter
import com.jehadalomour.flowvan.shared.presentation.feature.paymentreport.PaymentReportEvent
import com.jehadalomour.flowvan.shared.presentation.feature.paymentreport.PaymentReportViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PaymentReportScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenReceipt: (String) -> Unit = {},
    viewModel: PaymentReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text(
                    "تقرير المدفوعات",
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DateRangeBar(
                        fromMillis = state.fromMillis,
                        toMillis = state.toMillis,
                        onRangeSelected = { from, to ->
                            viewModel.onEvent(PaymentReportEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    FilterChipRow(
                        filters = PaymentMethodFilter.entries,
                        selected = state.methodFilter,
                        label = { filter ->
                            when (filter) {
                                PaymentMethodFilter.ALL -> "الكل"
                                PaymentMethodFilter.CASH -> "نقداً"
                                PaymentMethodFilter.CHEQUE -> "شيك"
                                PaymentMethodFilter.TRANSFER -> "تحويل"
                            }
                        },
                        onSelect = { viewModel.onEvent(PaymentReportEvent.MethodFilterChanged(it)) },
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryPill(
                            label = "الإجمالي",
                            value = state.total.formatJod(AppLanguage.AR),
                            accent = Fv.Blue,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryPill(
                            label = "المؤكدة",
                            value = state.confirmedTotal.formatJod(AppLanguage.AR),
                            accent = Fv.Green,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fv.Blue)
                        }
                    }
                } else if (state.payments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد مدفوعات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(state.payments, key = { it.id }) { payment ->
                        PayReportRow(payment, onClick = { onOpenReceipt(payment.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PayReportRow(p: PaymentEntity, onClick: () -> Unit) {
    val (methodLabel, methodColor) = when (p.method) {
        "CASH" -> "نقداً" to Fv.Green
        "CHEQUE" -> "شيك" to Fv.Amber
        "TRANSFER" -> "تحويل" to Fv.Blue
        else -> p.method to Fv.TextMid
    }
    val statusColor = when (p.status) {
        "BOUNCED" -> Fv.Red
        "PENDING" -> Fv.Amber
        else -> Fv.Green
    }
    val statusLabel = when (p.status) {
        "BOUNCED" -> "مرتجع"
        "PENDING" -> "معلق"
        else -> "مؤكد"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(methodColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) { Text(methodLabel, color = methodColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(p.createdAt.toDateTimeString(), color = Fv.TextMid, fontSize = 10.sp)
                }
                Text(p.amount.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            if (p.method == "CHEQUE" && (p.chequeNumber != null || p.chequeBank != null)) {
                Spacer(Modifier.height(6.dp))
                Text("شيك ${p.chequeNumber ?: "-"} · ${p.chequeBank ?: "-"}", color = Fv.TextMid, fontSize = 10.sp)
            }
            if (p.method == "TRANSFER" && p.transferRef != null) {
                Spacer(Modifier.height(4.dp))
                Text("مرجع: ${p.transferRef}", color = Fv.TextMid, fontSize = 10.sp)
            }
        }
    }
}
