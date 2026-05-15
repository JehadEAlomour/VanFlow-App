package com.jehadalomour.flowvan.screens.customer

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.jehadalomour.flowvan.screens.components.ChurnChip
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.SegmentChip
import com.jehadalomour.flowvan.screens.components.TierBadge
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardEvent
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerTab
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CustomerDashboardScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenSale: (String) -> Unit,
    onOpenReturn: (String) -> Unit,
    onOpenRequest: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenAi: (String) -> Unit,
    onOpenTransactionReport: (String) -> Unit,
    onOpenPaymentReport: (String) -> Unit,
    onOpenAccountStatement: (String) -> Unit,
    viewModel: CustomerDashboardViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                        Text(
                            "بطاقة العميل",
                            color = Fv.TextHigh,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onOpenAi(customerId) }) {
                            Text("✨", color = Fv.Purple, fontSize = 20.sp)
                        }
                    }
                }
                item { CustomerHeader(state) }
                item {
                    ReportButtons(
                        salesTotal = state.salesTotal,
                        returnsTotal = state.returnsTotal,
                        collectionsTotal = state.collectionsTotal,
                        onOpenTransactionReport = { onOpenTransactionReport(customerId) },
                        onOpenPaymentReport = { onOpenPaymentReport(customerId) },
                        onOpenAccountStatement = { onOpenAccountStatement(customerId) },
                    )
                }
                item { TabRow(state.selectedTab) { viewModel.onEvent(CustomerDashboardEvent.TabSelected(it)) } }
                tabContent(state)
            }
            BottomActionBar(
                onSale = { onOpenSale(customerId) },
                onReturn = { onOpenReturn(customerId) },
                onRequest = { onOpenRequest(customerId) },
                onCollection = { onOpenCollection(customerId) },
            )
        }
    }
}

@Composable
private fun CustomerHeader(state: com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardState) {
    val c = state.customer ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.nameAr, color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${c.code} · ${c.area}", color = Fv.TextMid, fontSize = 12.sp)
                }
                TierBadge(c.tier)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegmentChip(c.segment, c.churnRisk)
                if (c.churnRisk >= 0.60) ChurnChip(c.churnRisk)
            }
            Spacer(Modifier.height(12.dp))
            FinancialsBlock(
                balance = c.balance,
                overdue = c.overdueAmount,
                creditLimit = c.creditLimit,
            )
        }
    }
}

@Composable
private fun FinancialsBlock(balance: Double, overdue: Double, creditLimit: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FinancialPill("الرصيد", balance.formatJod(AppLanguage.AR), Fv.TextHigh, Modifier.weight(1f))
        FinancialPill("متأخر", overdue.formatJod(AppLanguage.AR), if (overdue > 0) Fv.Red else Fv.TextMid, Modifier.weight(1f))
        FinancialPill("سقف", creditLimit.formatJod(AppLanguage.AR), Fv.TextMid, Modifier.weight(1f))
    }
}

@Composable
private fun FinancialPill(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Fv.SurfaceHigh, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(label, color = Fv.TextMid, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportButtons(
    salesTotal: Double,
    returnsTotal: Double,
    collectionsTotal: Double,
    onOpenTransactionReport: () -> Unit,
    onOpenPaymentReport: () -> Unit,
    onOpenAccountStatement: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportButton(
                label = "تقرير المعاملات",
                value = salesTotal.formatJod(AppLanguage.AR),
                accent = Fv.Blue,
                modifier = Modifier.weight(1f),
                onClick = onOpenTransactionReport,
            )
            ReportButton(
                label = "تقرير المدفوعات",
                value = collectionsTotal.formatJod(AppLanguage.AR),
                accent = Fv.Green,
                modifier = Modifier.weight(1f),
                onClick = onOpenPaymentReport,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAccountStatement),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("كشف الحساب", color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("جميع الحركات والمديونية", color = Fv.TextMid, fontSize = 10.sp)
                }
                Text("←", color = Fv.Purple, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ReportButton(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = Fv.TextMid, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("عرض التقرير ←", color = accent.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun TabRow(selected: CustomerTab, onSelect: (CustomerTab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CustomerTab.values().toList()) { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .background(if (active) Fv.Blue else Fv.Surface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    tabLabel(tab),
                    color = if (active) Fv.TextHigh else Fv.TextMid,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

private fun tabLabel(tab: CustomerTab): String = when (tab) {
    CustomerTab.SUMMARY -> "الملخص"
    CustomerTab.SALES -> "المبيعات"
    CustomerTab.RETURNS -> "المرتجعات"
    CustomerTab.REQUESTS -> "الطلبات"
    CustomerTab.COLLECTIONS -> "التحصيلات"
}

private fun androidx.compose.foundation.lazy.LazyListScope.tabContent(
    state: com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardState,
) {
    when (state.selectedTab) {
        CustomerTab.SUMMARY -> {
            item { SummaryCard(state) }
        }
        CustomerTab.SALES -> renderInvoices(state.sales, "لا توجد فواتير بيع", typeColor = Fv.Green, typeLabel = "بيع")
        CustomerTab.RETURNS -> renderInvoices(state.returns, "لا توجد مرتجعات", typeColor = Fv.Red, typeLabel = "مرتجع")
        CustomerTab.REQUESTS -> renderInvoices(state.requests, "لا توجد طلبات", typeColor = Fv.Teal, typeLabel = "طلب")
        CustomerTab.COLLECTIONS -> renderPayments(state.payments)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderInvoices(
    list: List<InvoiceEntity>,
    emptyText: String,
    typeColor: androidx.compose.ui.graphics.Color,
    typeLabel: String,
) {
    if (list.isEmpty()) {
        item { EmptyState(emptyText) }
    } else {
        items(list, key = { it.id }) { invoice ->
            InvoiceRow(invoice, typeColor, typeLabel)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderPayments(list: List<PaymentEntity>) {
    if (list.isEmpty()) {
        item { EmptyState("لا توجد تحصيلات") }
    } else {
        items(list, key = { it.id }) { p -> PaymentRow(p) }
    }
}

@Composable
private fun SummaryCard(state: com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryRow("عدد الفواتير", "${state.sales.size + state.returns.size + state.requests.size}")
            SummaryRow("عدد التحصيلات", "${state.payments.size}")
            SummaryRow("الرصيد", state.customer?.balance?.formatJod(AppLanguage.AR) ?: "-")
            SummaryRow("متأخر", state.customer?.overdueAmount?.formatJod(AppLanguage.AR) ?: "-")
            SummaryRow("سقف الائتمان", state.customer?.creditLimit?.formatJod(AppLanguage.AR) ?: "-")
            SummaryRow("الرقم الضريبي", state.customer?.taxNumber ?: "-")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InvoiceRow(invoice: InvoiceEntity, typeColor: androidx.compose.ui.graphics.Color, typeLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invoice.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(invoice.paymentMethod ?: "—", color = Fv.TextMid, fontSize = 10.sp)
            }
            Text(invoice.total.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentRow(p: PaymentEntity) {
    val statusColor = when (p.status) {
        "BOUNCED" -> Fv.Red
        "PENDING" -> Fv.Amber
        else -> Fv.Green
    }
    val statusLabel = when (p.status) {
        "BOUNCED" -> "مرتجع"
        "PENDING" -> "معلق"
        else -> "مؤكد"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(methodLabel(p.method), color = Fv.TextMid, fontSize = 10.sp)
                }
                Text(p.amount.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            if (p.method == "CHEQUE" && (p.chequeNumber != null || p.chequeBank != null)) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "شيك ${p.chequeNumber ?: "-"} · ${p.chequeBank ?: "-"}",
                    color = Fv.TextMid, fontSize = 10.sp,
                )
            }
        }
    }
}

private fun methodLabel(method: String): String = when (method) {
    "CASH" -> "نقداً"
    "CHEQUE" -> "شيك"
    "TRANSFER" -> "تحويل"
    "CREDIT" -> "آجل"
    else -> method
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Fv.TextMid, fontSize = 13.sp) }
}

@Composable
private fun BottomActionBar(
    onSale: () -> Unit, onReturn: () -> Unit, onRequest: () -> Unit, onCollection: () -> Unit,
) {
    Surface(color = Fv.Surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton("بيع", Fv.Green, Modifier.weight(1f), onSale)
            ActionButton("مرتجع", Fv.Red, Modifier.weight(1f), onReturn)
            ActionButton("طلب", Fv.Teal, Modifier.weight(1f), onRequest)
            ActionButton("تحصيل", Fv.Amber, Modifier.weight(1f), onCollection)
        }
    }
}

@Composable
private fun ActionButton(label: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
