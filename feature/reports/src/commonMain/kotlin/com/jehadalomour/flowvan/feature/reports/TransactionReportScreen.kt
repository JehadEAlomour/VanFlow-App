package com.jehadalomour.flowvan.feature.reports

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.data.repository.CustomerTxn
import com.jehadalomour.flowvan.core.data.repository.TxnKind
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TransactionReportScreen(
    customerId: String,
    onBack: () -> Unit,
    /** Open the printable/shareable copy for the range currently on screen. */
    onPrint: (fromMillis: Long, toMillis: Long) -> Unit = { _, _ -> },
    viewModel: TransactionReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.txn_report_title),
                        color = Fv.TextHigh,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.customerNameAr.isNotBlank()) {
                        Text(state.customerNameAr, color = Fv.TextMid, fontSize = 11.sp)
                    }
                }
                // Disabled while the report is unbuilt: printing an empty page
                // because the network was down is worse than no button.
                val canPrint = !state.isLoading && state.errorAr == null
                IconButton(
                    onClick = { onPrint(state.fromMillis, state.toMillis) },
                    enabled = canPrint,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_print),
                        contentDescription = stringResource(Res.string.txn_report_print_action),
                        tint = if (canPrint) Fv.Blue else Fv.TextMid,
                        modifier = Modifier.size(22.dp),
                    )
                }
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
                            viewModel.onEvent(TransactionReportEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    val labelAll = stringResource(Res.string.all_sales_filter_all)
                    val labelSales = stringResource(Res.string.all_sales_filter_sales)
                    val labelReturns = stringResource(Res.string.all_sales_filter_returns)
                    val labelCollections = stringResource(Res.string.txn_report_filter_collections)
                    FilterChipRow(
                        filters = TxnTypeFilter.entries,
                        selected = state.typeFilter,
                        label = { filter ->
                            when (filter) {
                                TxnTypeFilter.ALL -> labelAll
                                TxnTypeFilter.SALE -> labelSales
                                TxnTypeFilter.RETURN -> labelReturns
                                TxnTypeFilter.COLLECTION -> labelCollections
                            }
                        },
                        onSelect = { viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(it)) },
                    )
                }

                item { TotalsCard(state) }

                when {
                    state.isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fv.Blue)
                        }
                    }

                    state.errorAr != null -> item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    state.errorAr!!,
                                    color = Fv.Amber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    onClick = { viewModel.onEvent(TransactionReportEvent.Retry) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Fv.Blue,
                                ) {
                                    Text(
                                        stringResource(Res.string.txn_report_retry),
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    state.rows.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(Res.string.txn_report_empty),
                                color = Fv.TextMid,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    else -> items(state.rows, key = { "${it.kind}-${it.id}" }) { txn ->
                        TxnRow(txn)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(state: TransactionReportState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill(
                    label = stringResource(Res.string.all_sales_total_sales),
                    value = state.salesTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Green,
                    modifier = Modifier.weight(1f),
                )
                SummaryPill(
                    label = stringResource(Res.string.all_sales_total_returns),
                    value = state.returnsTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Red,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill(
                    label = stringResource(Res.string.txn_report_total_collections),
                    value = state.collectionsTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Blue,
                    modifier = Modifier.weight(1f),
                )
                SummaryPill(
                    label = stringResource(Res.string.txn_report_total_cash),
                    value = state.cashTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Teal,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Fv.SurfaceHigh)
            Spacer(Modifier.height(10.dp))
            // The two numbers the report exists to state: what moved, and how
            // much of it the customer still owes for.
            TotalRow(
                label = stringResource(Res.string.txn_report_net_total),
                value = state.netTotal.formatJod(AppLanguage.AR),
                accent = Fv.TextHigh,
            )
            Spacer(Modifier.height(6.dp))
            TotalRow(
                label = stringResource(Res.string.txn_report_total_credit),
                value = state.creditTotal.formatJod(AppLanguage.AR),
                accent = Fv.Amber,
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Fv.TextMid, fontSize = 12.sp)
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TxnRow(txn: CustomerTxn) {
    val (typeLabel, typeColor) = when (txn.kind) {
        TxnKind.SALE -> stringResource(Res.string.chip_sale) to Fv.Green
        TxnKind.RETURN -> stringResource(Res.string.chip_return) to Fv.Red
        TxnKind.ORDER -> stringResource(Res.string.chip_request) to Fv.Teal
        TxnKind.COLLECTION -> stringResource(Res.string.txn_report_collection) to Fv.Blue
    }
    // Cash vs credit is the question this report is opened to answer, so it is a
    // badge on the row rather than a number the reader has to derive.
    val settleLabel = when {
        txn.kind == TxnKind.COLLECTION -> when (txn.method) {
            "cash" -> stringResource(Res.string.method_cash_label)
            "cheque" -> stringResource(Res.string.method_cheque_label)
            "transfer" -> stringResource(Res.string.method_transfer_label)
            else -> null
        }
        txn.isCash -> stringResource(Res.string.txn_report_cash)
        else -> stringResource(Res.string.txn_report_credit)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(txn.date, color = Fv.TextMid, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    txn.total.formatJod(AppLanguage.AR),
                    color = Fv.TextHigh,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                settleLabel?.let { Text(it, color = Fv.TextMid, fontSize = 10.sp) }
            }
        }
    }
}
