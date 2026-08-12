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
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * تقرير المبيعات — the round's documents over a period.
 *
 * On [ReportKit], like every other report: the same chrome, the same totals block and
 * the same three states, so a rep who learns one report has learned all of them. What
 * is specific here is which figures matter — sales against returns, and how much of
 * the selling went out on credit rather than into the bag.
 *
 * It prints from the local database rather than the server, unlike تقرير الحركات. These
 * are the rep's own vouchers, written on this device, so the paper is correct in a
 * village with no signal — which is where the round ends.
 */
@Composable
fun AllSalesReportScreen(
    onBack: () -> Unit,
    onOpenVoucher: (String) -> Unit,
    onPrint: (fromMillis: Long, toMillis: Long) -> Unit = { _, _ -> },
    viewModel: AllSalesReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            ReportTopBar(
                title = stringResource(Res.string.all_sales_title),
                onBack = onBack,
                onPrint = { onPrint(state.from, state.to) },
                printEnabled = state.count > 0,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DateRangeBar(state.from, state.to) { f, t ->
                            viewModel.setFrom(f); viewModel.setTo(t)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    val labelAll = stringResource(Res.string.all_sales_filter_all)
                    val labelSales = stringResource(Res.string.all_sales_filter_sales)
                    val labelReturns = stringResource(Res.string.all_sales_filter_returns)
                    val labelRequests = stringResource(Res.string.all_sales_filter_requests)
                    ReportChipRow {
                        SalesTypeFilter.entries.forEach { filter ->
                            ReportChip(
                                label = when (filter) {
                                    SalesTypeFilter.ALL -> labelAll
                                    SalesTypeFilter.SALE -> labelSales
                                    SalesTypeFilter.RETURN -> labelReturns
                                    SalesTypeFilter.REQUEST -> labelRequests
                                },
                                selected = state.typeFilter == filter,
                                onClick = { viewModel.setTypeFilter(filter) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Totals describe the whole period, never the chip — see the ViewModel.
                item {
                    ReportTotals(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        figures = listOf(
                            ReportFigure(stringResource(Res.string.all_sales_total_sales), state.salesTotal.formatJod(AppLanguage.AR), Fv.Green),
                            ReportFigure(stringResource(Res.string.all_sales_total_returns), state.returnsTotal.formatJod(AppLanguage.AR), Fv.Red),
                            ReportFigure(stringResource(Res.string.txn_report_total_cash), state.cashTotal.formatJod(AppLanguage.AR), Fv.Teal),
                            ReportFigure(stringResource(Res.string.all_sales_count), state.count.toString(), Fv.TextMid),
                            ReportFigure(stringResource(Res.string.all_sales_pill_net), state.netTotal.formatJod(AppLanguage.AR), Fv.TextHigh, emphasis = true),
                            ReportFigure(stringResource(Res.string.txn_report_total_credit), state.creditTotal.formatJod(AppLanguage.AR), Fv.Amber, emphasis = true),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (state.rows.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) {
                            ReportEmpty(stringResource(Res.string.report_empty_period))
                        }
                    }
                } else {
                    items(state.rows, key = { it.id }) { inv ->
                        SalesInvoiceRow(
                            inv = inv,
                            customerName = state.customerNames[inv.customerId].orEmpty(),
                            onClick = { onOpenVoucher(inv.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesInvoiceRow(inv: InvoiceEntity, customerName: String, onClick: () -> Unit) {
    val (badge, accent) = when (inv.type) {
        "SALE" -> stringResource(Res.string.chip_sale) to Fv.Green
        "RETURN" -> stringResource(Res.string.chip_return) to Fv.Red
        else -> stringResource(Res.string.chip_request) to Fv.Teal
    }
    // Whose voucher it is leads the row: on a rep-level report the document number
    // identifies nothing a rep remembers, but the shop does.
    val title = customerName.ifBlank { inv.number }
    val subtitle = buildString {
        if (customerName.isNotBlank()) append("${inv.number} · ")
        append(inv.createdAt.toDateString())
    }
    // Cash or credit is the question this report is opened to settle, so it is stated
    // under the figure rather than left to be derived. A request is not paid at all.
    val caption = when {
        inv.type != "SALE" -> null
        inv.paymentMethod == "CREDIT" -> stringResource(Res.string.txn_report_credit)
        else -> stringResource(Res.string.txn_report_cash)
    }

    ReportRow(
        title = title,
        subtitle = subtitle,
        value = inv.total.formatJod(AppLanguage.AR),
        valueCaption = caption,
        edgeColor = accent,
        badge = badge,
        badgeColor = accent,
        onClick = onClick,
    )
}
