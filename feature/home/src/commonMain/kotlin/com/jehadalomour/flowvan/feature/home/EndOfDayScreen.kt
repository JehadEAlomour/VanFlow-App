package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.feature.home.EndOfDayEvent
import com.jehadalomour.flowvan.feature.home.EndOfDayViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EndOfDayScreen(
    onLoggedOut: () -> Unit,
    viewModel: EndOfDayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.done) { if (state.done) onLoggedOut() }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_moon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(start = 8.dp),
                )
                Text(
                    stringResource(Res.string.home_quick_end_of_day),
                    color = Fv.TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                state.kpi?.let { kpi ->
                    SectionCard(title = stringResource(Res.string.end_of_day_summary_title)) {
                        KpiRow(stringResource(Res.string.end_of_day_total_sales), kpi.salesTotal.formatJod(AppLanguage.AR), Fv.Green)
                        KpiRow(stringResource(Res.string.end_of_day_cash_sales), kpi.cashSalesTotal.formatJod(AppLanguage.AR), Fv.Green)
                        KpiRow(stringResource(Res.string.end_of_day_credit_sales), kpi.creditSalesTotal.formatJod(AppLanguage.AR), Fv.Amber)
                        KpiRow(stringResource(Res.string.end_of_day_total_returns), kpi.returnsTotal.formatJod(AppLanguage.AR), Fv.Red)
                        HorizontalDivider(color = Fv.SurfaceHigh, modifier = Modifier.padding(vertical = 4.dp))
                        KpiRow(stringResource(Res.string.end_of_day_net_sales), (kpi.salesTotal - kpi.returnsTotal).formatJod(AppLanguage.AR), Fv.TextHigh)
                        KpiRow(stringResource(Res.string.end_of_day_total_collections), kpi.collectionsTotal.formatJod(AppLanguage.AR), Fv.Amber)
                        Spacer(Modifier.height(4.dp))
                        KpiRow(
                            stringResource(Res.string.end_of_day_customers_visited),
                            "${kpi.customersVisited} / ${kpi.customersPlanned}",
                            if (kpi.customersVisited >= kpi.customersPlanned) Fv.Green else Fv.Amber,
                        )
                    }
                }

                SectionCard(title = stringResource(Res.string.end_of_day_cash_settlement)) {
                    KpiRow(stringResource(Res.string.method_cash_label), state.cashCollectedToday.formatJod(AppLanguage.AR), Fv.Green)
                    KpiRow(stringResource(Res.string.end_of_day_cheques), state.chequesCollectedToday.formatJod(AppLanguage.AR), Fv.Blue)
                    KpiRow(stringResource(Res.string.end_of_day_transfers), state.transfersCollectedToday.formatJod(AppLanguage.AR), Fv.Teal)
                }

                SectionCard(title = stringResource(Res.string.end_of_day_sync_status)) {
                    KpiRow(
                        stringResource(Res.string.end_of_day_unsynced_invoices),
                        state.unsyncedInvoices.toString(),
                        if (state.unsyncedInvoices == 0) Fv.Green else Fv.Amber,
                    )
                    KpiRow(
                        stringResource(Res.string.end_of_day_unsynced_payments),
                        state.unsyncedPayments.toString(),
                        if (state.unsyncedPayments == 0) Fv.Green else Fv.Amber,
                    )
                    if (state.unsyncedInvoices > 0 || state.unsyncedPayments > 0) {
                        Text(
                            stringResource(Res.string.end_of_day_sync_note),
                            color = Fv.TextMid,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.onEvent(EndOfDayEvent.OpenConfirmDialog) },
                enabled = !state.isEnding,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fv.Red,
                    contentColor = Fv.TextHigh,
                    disabledContainerColor = Fv.SurfaceTop,
                    disabledContentColor = Fv.TextMid,
                ),
            ) {
                Text(
                    if (state.isEnding) stringResource(Res.string.end_of_day_ending) else stringResource(Res.string.end_of_day_end_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EndOfDayEvent.DismissConfirmDialog) },
            title = { Text(stringResource(Res.string.end_of_day_confirm_end_title), color = Fv.TextHigh) },
            text = { Text(stringResource(Res.string.end_of_day_confirm_end_message), color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EndOfDayEvent.ConfirmEndShift) }) {
                    Text(stringResource(Res.string.confirm), color = Fv.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EndOfDayEvent.DismissConfirmDialog) }) {
                    Text(stringResource(Res.string.cancel), color = Fv.TextMid)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun KpiRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Fv.TextMid, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
