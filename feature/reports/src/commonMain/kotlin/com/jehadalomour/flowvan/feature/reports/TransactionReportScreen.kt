package com.jehadalomour.flowvan.feature.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.data.repository.CustomerTxn
import com.jehadalomour.flowvan.core.data.repository.TxnKind
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * تقرير الحركات — one customer's movement over a period, from the server.
 *
 * Built on [ReportKit] so its chrome, totals and three states match every other
 * report. The only thing local to this screen is which figures matter.
 */
@Composable
fun TransactionReportScreen(
    customerId: String,
    onBack: () -> Unit,
    onPrint: (fromMillis: Long, toMillis: Long) -> Unit = { _, _ -> },
    viewModel: TransactionReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            ReportTopBar(
                title = stringResource(Res.string.txn_report_title),
                subtitle = state.customerNameAr.takeIf { it.isNotBlank() },
                onBack = onBack,
                onPrint = { onPrint(state.fromMillis, state.toMillis) },
                printEnabled = !state.isLoading && state.errorAr == null,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DateRangeBar(
                            fromMillis = state.fromMillis,
                            toMillis = state.toMillis,
                            onRangeSelected = { from, to ->
                                viewModel.onEvent(TransactionReportEvent.DateRangeChanged(from, to))
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    val all = stringResource(Res.string.all_sales_filter_all)
                    val sales = stringResource(Res.string.all_sales_filter_sales)
                    val returns = stringResource(Res.string.all_sales_filter_returns)
                    val collections = stringResource(Res.string.txn_report_filter_collections)
                    ReportChipRow {
                        ReportChip(all, state.typeFilter == TxnTypeFilter.ALL) {
                            viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(TxnTypeFilter.ALL))
                        }
                        ReportChip(sales, state.typeFilter == TxnTypeFilter.SALE) {
                            viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(TxnTypeFilter.SALE))
                        }
                        ReportChip(returns, state.typeFilter == TxnTypeFilter.RETURN) {
                            viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(TxnTypeFilter.RETURN))
                        }
                        ReportChip(collections, state.typeFilter == TxnTypeFilter.COLLECTION) {
                            viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(TxnTypeFilter.COLLECTION))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Totals always describe the WHOLE period, never the filtered
                // view — a chip is a way of looking, not a claim about what the
                // customer transacted.
                item {
                    ReportTotals(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        figures = listOf(
                            ReportFigure(stringResource(Res.string.all_sales_total_sales), state.salesTotal.formatJod(AppLanguage.AR), Fv.Green),
                            ReportFigure(stringResource(Res.string.all_sales_total_returns), state.returnsTotal.formatJod(AppLanguage.AR), Fv.Red),
                            ReportFigure(stringResource(Res.string.txn_report_total_collections), state.collectionsTotal.formatJod(AppLanguage.AR), Fv.Blue),
                            ReportFigure(stringResource(Res.string.txn_report_total_cash), state.cashTotal.formatJod(AppLanguage.AR), Fv.Teal),
                            ReportFigure(stringResource(Res.string.txn_report_net_total), state.netTotal.formatJod(AppLanguage.AR), Fv.TextHigh, emphasis = true),
                            ReportFigure(stringResource(Res.string.txn_report_total_credit), state.creditTotal.formatJod(AppLanguage.AR), Fv.Amber, emphasis = true),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    state.isLoading -> item { Box(Modifier.fillMaxWidth().height(160.dp)) { ReportLoading() } }

                    state.errorAr != null -> item {
                        Box(Modifier.fillMaxWidth().height(220.dp)) {
                            ReportError(state.errorAr!!) {
                                viewModel.onEvent(TransactionReportEvent.Retry)
                            }
                        }
                    }

                    state.rows.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) {
                            ReportEmpty(stringResource(Res.string.txn_report_empty))
                        }
                    }

                    else -> items(state.rows, key = { "${it.kind}-${it.id}" }) { txn -> TxnRow(txn) }
                }
            }
        }
    }
}

@Composable
private fun TxnRow(txn: CustomerTxn) {
    val (badge, color) = when (txn.kind) {
        TxnKind.SALE -> stringResource(Res.string.chip_sale) to Fv.Green
        TxnKind.RETURN -> stringResource(Res.string.chip_return) to Fv.Red
        TxnKind.ORDER -> stringResource(Res.string.chip_request) to Fv.Teal
        TxnKind.COLLECTION -> stringResource(Res.string.txn_report_collection) to Fv.Blue
    }
    // Cash vs credit is the question this report is opened to answer, so it sits
    // under the figure rather than being left for the reader to derive.
    val caption = when {
        txn.kind == TxnKind.COLLECTION -> when (txn.method) {
            "cash" -> stringResource(Res.string.method_cash_label)
            "cheque" -> stringResource(Res.string.method_cheque_label)
            "transfer" -> stringResource(Res.string.method_transfer_label)
            else -> null
        }
        txn.isCash -> stringResource(Res.string.txn_report_cash)
        else -> stringResource(Res.string.txn_report_credit)
    }

    ReportRow(
        title = txn.number,
        subtitle = txn.date,
        value = txn.total.formatJod(AppLanguage.AR),
        valueCaption = caption,
        edgeColor = color,
        badge = badge,
        badgeColor = color,
    )
}
