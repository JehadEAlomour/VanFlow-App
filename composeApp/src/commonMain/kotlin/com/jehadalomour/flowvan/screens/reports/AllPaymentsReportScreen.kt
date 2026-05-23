package com.jehadalomour.flowvan.screens.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.presentation.feature.reports.AllPaymentMethodFilter
import com.jehadalomour.flowvan.shared.presentation.feature.reports.AllPaymentsReportViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllPaymentsReportScreen(
    onBack: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    viewModel: AllPaymentsReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = null,
                            tint = Fv.TextHigh,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.all_payments_title), color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                DateRangeBar(state.from, state.to) { f, t ->
                    viewModel.setFrom(f); viewModel.setTo(t)
                }
            }
            item {
                val allLabel = stringResource(Res.string.chip_all)
                val cashLabel = stringResource(Res.string.chip_cash)
                val chequeLabel = stringResource(Res.string.chip_cheque)
                val transferLabel = stringResource(Res.string.chip_transfer)
                FilterChipRow(
                    filters = AllPaymentMethodFilter.entries,
                    selected = state.methodFilter,
                    label = {
                        when (it) {
                            AllPaymentMethodFilter.ALL -> allLabel
                            AllPaymentMethodFilter.CASH -> cashLabel
                            AllPaymentMethodFilter.CHEQUE -> chequeLabel
                            AllPaymentMethodFilter.TRANSFER -> transferLabel
                        }
                    },
                    onSelect = { viewModel.setMethodFilter(it) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryPill(stringResource(Res.string.chip_cash), state.cashTotal.formatJod(AppLanguage.AR), Fv.Green, Modifier.weight(1f))
                    SummaryPill(stringResource(Res.string.chip_cheque), state.chequeTotal.formatJod(AppLanguage.AR), Fv.Blue, Modifier.weight(1f))
                    SummaryPill(stringResource(Res.string.chip_transfer), state.transferTotal.formatJod(AppLanguage.AR), Fv.Purple, Modifier.weight(1f))
                }
            }
            item {
                SummaryPill(stringResource(Res.string.total), state.total.formatJod(AppLanguage.AR), Fv.Amber, Modifier.fillMaxWidth())
            }
            if (state.payments.isEmpty()) {
                item { Text(stringResource(Res.string.all_payments_empty), color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp)) }
            } else {
                items(state.payments, key = { it.id }) { pay ->
                    PaymentRow(pay, onClick = { onOpenReceipt(pay.id) })
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(pay: PaymentEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val methodLabel = when (pay.method) {
                "CASH" -> stringResource(Res.string.chip_cash)
                "CHEQUE" -> stringResource(Res.string.chip_cheque)
                "TRANSFER" -> stringResource(Res.string.chip_transfer)
                else -> pay.method
            }
            val methodColor = when (pay.method) {
                "CASH" -> Fv.Green
                "CHEQUE" -> Fv.Blue
                else -> Fv.Purple
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(pay.number, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(pay.createdAt.toDateString(), color = Fv.TextMid, fontSize = 11.sp)
            }
            Text(methodLabel, color = methodColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp))
            Text(pay.amount.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
