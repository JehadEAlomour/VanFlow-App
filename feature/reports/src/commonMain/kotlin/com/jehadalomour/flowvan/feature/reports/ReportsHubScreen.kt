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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Text(stringResource(Res.string.reports_title), color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_receipt),
                    title = stringResource(Res.string.reports_sales_title),
                    subtitle = stringResource(Res.string.reports_sales_subtitle),
                    accent = Fv.Blue,
                    onClick = onOpenSalesReport,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_payment),
                    title = stringResource(Res.string.reports_payments_title),
                    subtitle = stringResource(Res.string.reports_payments_subtitle),
                    accent = Fv.Green,
                    onClick = onOpenPaymentsReport,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_map),
                    title = stringResource(Res.string.reports_visits_title),
                    subtitle = stringResource(Res.string.reports_visits_subtitle),
                    accent = Fv.Teal,
                    onClick = onOpenVisitReport,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_bar_chart),
                    title = stringResource(Res.string.reports_cash_flow_title),
                    subtitle = stringResource(Res.string.reports_cash_flow_subtitle),
                    accent = Fv.Amber,
                    onClick = onOpenCashFlow,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_inventory),
                    title = stringResource(Res.string.reports_items_title),
                    subtitle = stringResource(Res.string.reports_items_subtitle),
                    accent = Fv.Purple,
                    onClick = onOpenItemsSales,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_warning),
                    title = stringResource(Res.string.reports_receivables_title),
                    subtitle = stringResource(Res.string.reports_receivables_subtitle),
                    accent = Fv.Red,
                    onClick = onOpenReceivables,
                )
            }
            item {
                ReportCard(
                    icon = painterResource(Res.drawable.ic_bar_chart),
                    title = stringResource(Res.string.targets_title),
                    subtitle = stringResource(Res.string.targets_hub_subtitle),
                    accent = Fv.Green,
                    onClick = onOpenTargets,
                )
            }
        }
    }
}

@Composable
private fun ReportCard(icon: Painter, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Fv.TextMid, fontSize = 12.sp)
            }
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Fv.TextMid,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

