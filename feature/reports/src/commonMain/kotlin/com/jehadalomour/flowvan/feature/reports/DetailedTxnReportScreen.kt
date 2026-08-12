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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.data.repository.DetailedTxnDoc
import com.jehadalomour.flowvan.core.data.repository.TxnKind
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailedTxnReportScreen(
    customerId: String,
    onBack: () -> Unit,
    onPrint: (fromMillis: Long, toMillis: Long) -> Unit = { _, _ -> },
    viewModel: DetailedTxnReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    val report = state.report

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReportTopBar(
                title = stringResource(Res.string.detailed_txn_title),
                subtitle = state.customerNameAr.takeIf { it.isNotBlank() },
                onBack = onBack,
                onPrint = { onPrint(state.fromMillis, state.toMillis) },
                printEnabled = !state.isLoading && state.errorAr == null,
            )

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
                            viewModel.onEvent(DetailedTxnReportEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    ReportTotals(
                        figures = listOf(
                            ReportFigure(stringResource(Res.string.all_sales_total_sales), report.salesTotal.formatJod(AppLanguage.AR), Fv.Green),
                            ReportFigure(stringResource(Res.string.all_sales_total_returns), report.returnsTotal.formatJod(AppLanguage.AR), Fv.Red),
                            ReportFigure(stringResource(Res.string.txn_report_total_collections), report.collectionsTotal.formatJod(AppLanguage.AR), Fv.Blue),
                            ReportFigure(stringResource(Res.string.detailed_txn_lines), report.lineCount.toString(), Fv.Teal),
                            ReportFigure(stringResource(Res.string.txn_report_net_total), report.netTotal.formatJod(AppLanguage.AR), Fv.TextHigh, emphasis = true),
                            ReportFigure(stringResource(Res.string.txn_report_total_credit), report.creditTotal.formatJod(AppLanguage.AR), Fv.Amber, emphasis = true),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (report.docs.isNotEmpty() && state.errorAr == null && !state.isLoading) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallButton(
                                stringResource(Res.string.detailed_txn_expand_all),
                                Modifier.weight(1f),
                            ) { viewModel.onEvent(DetailedTxnReportEvent.ExpandAll) }
                            SmallButton(
                                stringResource(Res.string.detailed_txn_collapse_all),
                                Modifier.weight(1f),
                            ) { viewModel.onEvent(DetailedTxnReportEvent.CollapseAll) }
                        }
                    }
                }

                when {
                    state.isLoading -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) { ReportLoading() }
                    }

                    state.errorAr != null -> item {
                        Box(Modifier.fillMaxWidth().height(220.dp)) {
                            ReportError(state.errorAr!!) {
                                viewModel.onEvent(DetailedTxnReportEvent.Retry)
                            }
                        }
                    }

                    report.docs.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) {
                            ReportEmpty(stringResource(Res.string.txn_report_empty))
                        }
                    }

                    else -> items(report.docs, key = { it.id }) { doc ->
                        DocCard(
                            doc = doc,
                            expanded = doc.id in state.expanded,
                            onToggle = {
                                viewModel.onEvent(DetailedTxnReportEvent.ToggleExpanded(doc.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Fv.Surface,
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 10.dp),
            color = Fv.Blue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun DocCard(doc: DetailedTxnDoc, expanded: Boolean, onToggle: () -> Unit) {
    val (typeLabel, typeColor) = when (doc.kind) {
        TxnKind.SALE -> stringResource(Res.string.chip_sale) to Fv.Green
        TxnKind.RETURN -> stringResource(Res.string.chip_return) to Fv.Red
        TxnKind.ORDER -> stringResource(Res.string.chip_request) to Fv.Teal
        TxnKind.COLLECTION -> stringResource(Res.string.txn_report_collection) to Fv.Blue
    }
    val settleLabel = when {
        doc.kind == TxnKind.COLLECTION -> null
        doc.isCash -> stringResource(Res.string.txn_report_cash)
        else -> stringResource(Res.string.txn_report_credit)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    // A collection has no lines to open, so it is not clickable —
                    // a card that expands to nothing reads as a broken one.
                    .then(if (doc.lines.isEmpty()) Modifier else Modifier.clickable(onClick = onToggle))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) { Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(doc.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(doc.date, color = Fv.TextMid, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        doc.total.formatJod(AppLanguage.AR),
                        color = Fv.TextHigh,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val sub = settleLabel?.let { s ->
                        if (doc.lines.isEmpty()) s else "$s · ${doc.lines.size}"
                    }
                    sub?.let { Text(it, color = Fv.TextMid, fontSize = 10.sp) }
                }
            }

            if (expanded && doc.lines.isNotEmpty()) {
                HorizontalDivider(color = Fv.SurfaceHigh)
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    doc.lines.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    line.itemName,
                                    color = Fv.TextHigh,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    buildString {
                                        append(line.qty.trimZeros())
                                        line.unit?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                                        append(" × ")
                                        append(line.unitPrice.formatJod(AppLanguage.AR))
                                    },
                                    color = Fv.TextMid,
                                    fontSize = 10.sp,
                                )
                            }
                            Text(
                                line.lineTotal.formatJod(AppLanguage.AR),
                                color = Fv.TextHigh,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 3.0 → "3", 2.5 → "2.5". Quantities read badly with trailing zeros. */
private fun Double.trimZeros(): String {
    val whole = this.toLong()
    return if (this == whole.toDouble()) whole.toString() else this.toString()
}
