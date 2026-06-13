package com.jehadalomour.flowvan.feature.reports

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.feature.reports.PaymentMethodFilter
import com.jehadalomour.flowvan.feature.reports.PaymentReportEvent
import com.jehadalomour.flowvan.feature.reports.PaymentReportViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
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
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    stringResource(Res.string.payment_report_payments_title),
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
                    val allLabel = stringResource(Res.string.chip_all)
                    val cashLabel = stringResource(Res.string.method_cash_label)
                    val chequeLabel = stringResource(Res.string.method_cheque_label)
                    val transferLabel = stringResource(Res.string.method_transfer_label)
                    FilterChipRow(
                        filters = PaymentMethodFilter.entries,
                        selected = state.methodFilter,
                        label = { filter ->
                            when (filter) {
                                PaymentMethodFilter.ALL -> allLabel
                                PaymentMethodFilter.CASH -> cashLabel
                                PaymentMethodFilter.CHEQUE -> chequeLabel
                                PaymentMethodFilter.TRANSFER -> transferLabel
                            }
                        },
                        onSelect = { viewModel.onEvent(PaymentReportEvent.MethodFilterChanged(it)) },
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryPill(
                            label = stringResource(Res.string.total),
                            value = state.total.formatJod(AppLanguage.AR),
                            accent = Fv.Blue,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryPill(
                            label = stringResource(Res.string.payment_report_confirmed),
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
                            Text(stringResource(Res.string.payment_report_empty), color = Fv.TextMid, fontSize = 13.sp)
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
        "CASH" -> stringResource(Res.string.method_cash_label) to Fv.Green
        "CHEQUE" -> stringResource(Res.string.method_cheque_label) to Fv.Amber
        "TRANSFER" -> stringResource(Res.string.method_transfer_label) to Fv.Blue
        else -> p.method to Fv.TextMid
    }
    val statusColor = when (p.status) {
        "BOUNCED" -> Fv.Red
        "PENDING" -> Fv.Amber
        else -> Fv.Green
    }
    val statusLabel = when (p.status) {
        "BOUNCED" -> stringResource(Res.string.receipt_status_bounced)
        "PENDING" -> stringResource(Res.string.payment_status_pending)
        else -> stringResource(Res.string.receipt_status_confirmed)
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
                Text(stringResource(Res.string.payment_cheque_line, p.chequeNumber ?: "-", p.chequeBank ?: "-"), color = Fv.TextMid, fontSize = 10.sp)
            }
            if (p.method == "TRANSFER" && p.transferRef != null) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(Res.string.payment_ref_line, p.transferRef ?: "-"), color = Fv.TextMid, fontSize = 10.sp)
            }
        }
    }
}
