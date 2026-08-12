package com.jehadalomour.flowvan.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.FvGridTile
import com.jehadalomour.flowvan.core.designsystem.components.ReportTopBar
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * التقارير — a launcher, not a feed.
 *
 * Was a scrolling column of description cards: eight reports, two visible at a
 * time, each explaining itself in a sentence nobody reads twice. Now a 3-column
 * grid where every report is reachable without scrolling, and the label is the
 * description.
 */
@Composable
fun ReportsHubScreen(
    onBack: () -> Unit,
    onOpenSalesReport: () -> Unit,
    onOpenPaymentsReport: () -> Unit,
    onOpenVisitReport: () -> Unit,
    onOpenCashFlow: () -> Unit,
    onOpenItemsSales: () -> Unit,
    onOpenReceivables: () -> Unit,
    onOpenTargets: () -> Unit,
    onOpenVoucherSummary: () -> Unit,
) {
    // Ordered by how often a rep opens them, not alphabetically or by module.
    val tiles = listOf(
        ReportTile(Res.drawable.ic_receipt, stringResource(Res.string.reports_sales_title), Fv.Green, onOpenSalesReport),
        ReportTile(Res.drawable.ic_payment, stringResource(Res.string.reports_payments_title), Fv.Blue, onOpenPaymentsReport),
        ReportTile(Res.drawable.ic_customers, stringResource(Res.string.reports_receivables_title), Fv.Red, onOpenReceivables),
        ReportTile(Res.drawable.ic_inventory, stringResource(Res.string.reports_items_title), Fv.Teal, onOpenItemsSales),
        ReportTile(Res.drawable.ic_bar_chart, stringResource(Res.string.reports_cash_flow_title), Fv.Blue, onOpenCashFlow),
        ReportTile(Res.drawable.ic_map, stringResource(Res.string.reports_visits_title), Fv.Teal, onOpenVisitReport),
        ReportTile(Res.drawable.ic_check_circle, stringResource(Res.string.targets_title), Fv.Amber, onOpenTargets),
        ReportTile(Res.drawable.ic_receipt, stringResource(Res.string.voucher_summary_title), Fv.TextMid, onOpenVoucherSummary),
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            ReportTopBar(title = stringResource(Res.string.reports_title), onBack = onBack)

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tiles) { tile ->
                    FvGridTile(
                        icon = painterResource(tile.icon),
                        label = tile.label,
                        accent = tile.accent,
                        modifier = Modifier.aspectRatio(1f),
                        onClick = tile.onClick,
                    )
                }
            }
        }
    }
}

private data class ReportTile(
    val icon: DrawableResource,
    val label: String,
    val accent: Color,
    val onClick: () -> Unit,
)
