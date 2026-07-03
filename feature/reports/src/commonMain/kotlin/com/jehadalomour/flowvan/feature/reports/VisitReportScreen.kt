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
import androidx.compose.foundation.shape.CircleShape
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
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.feature.reports.VisitReportViewModel
import com.jehadalomour.flowvan.feature.reports.VisitedCustomer
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VisitReportScreen(
    onBack: () -> Unit,
    viewModel: VisitReportViewModel = koinViewModel(),
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
                    Text(stringResource(Res.string.visits_title), color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                DateRangeBar(state.from, state.to) { f, t ->
                    viewModel.setFrom(f); viewModel.setTo(t)
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill(stringResource(Res.string.visits_pill_visited), "${state.visitedCount}", Fv.Green, Modifier.weight(1f))
                            SummaryPill(stringResource(Res.string.visits_pill_remaining), "${state.plannedCount - state.visitedCount}", Fv.Red, Modifier.weight(1f))
                            SummaryPill(stringResource(Res.string.total), "${state.plannedCount}", Fv.Blue, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        val rate = (state.visitRate * 100).toInt()
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.visits_rate_label), color = Fv.TextMid, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(Fv.SurfaceHigh, CircleShape),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(state.visitRate.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .background(if (rate >= 80) Fv.Green else if (rate >= 50) Fv.Amber else Fv.Red, CircleShape),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$rate%", color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(Res.string.visits_total_sales_value, state.totalSales.formatJod(AppLanguage.AR)), color = Fv.TextMid, fontSize = 12.sp)
                    }
                }
            }
            items(state.customers, key = { it.customer.id }) { vc ->
                VisitedCustomerRow(vc)
            }
        }
    }
}

@Composable
private fun VisitedCustomerRow(vc: VisitedCustomer) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (vc.visited) Fv.Green.copy(alpha = 0.15f) else Fv.Red.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (vc.visited) Res.drawable.ic_check else Res.drawable.ic_radio_button_off),
                    contentDescription = null,
                    tint = if (vc.visited) Fv.Green else Fv.Red,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vc.customer.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (!vc.onRoute) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Fv.Amber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                stringResource(Res.string.visits_off_route),
                                color = Fv.Amber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(vc.customer.area, color = Fv.TextMid, fontSize = 11.sp)
            }
            if (vc.visited) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(vc.salesTotal.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.visits_invoice_count, vc.invoiceCount), color = Fv.TextMid, fontSize = 10.sp)
                }
            } else {
                Text(stringResource(Res.string.visits_not_visited), color = Fv.Red, fontSize = 11.sp)
            }
        }
    }
}
