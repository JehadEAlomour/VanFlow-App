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
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.presentation.feature.reports.AllSalesReportViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.reports.SalesTypeFilter
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllSalesReportScreen(
    onBack: () -> Unit,
    onOpenVoucher: (String) -> Unit,
    viewModel: AllSalesReportViewModel = koinViewModel(),
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
                    Text("تقرير المبيعات", color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                DateRangeBar(state.from, state.to) { f, t ->
                    viewModel.setFrom(f); viewModel.setTo(t)
                }
            }
            item {
                FilterChipRow(
                    filters = SalesTypeFilter.entries,
                    selected = state.typeFilter,
                    label = {
                        when (it) {
                            SalesTypeFilter.ALL -> "الكل"
                            SalesTypeFilter.SALE -> "مبيعات"
                            SalesTypeFilter.RETURN -> "مرتجعات"
                            SalesTypeFilter.REQUEST -> "طلبات"
                        }
                    },
                    onSelect = { viewModel.setTypeFilter(it) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryPill("المبيعات", state.salesTotal.formatJod(AppLanguage.AR), Fv.Blue, Modifier.weight(1f))
                    SummaryPill("المرتجعات", state.returnsTotal.formatJod(AppLanguage.AR), Fv.Red, Modifier.weight(1f))
                }
            }
            item {
                SummaryPill(
                    "الصافي",
                    (state.salesTotal - state.returnsTotal).formatJod(AppLanguage.AR),
                    Fv.Green,
                    Modifier.fillMaxWidth(),
                )
            }
            if (state.invoices.isEmpty()) {
                item { Text("لا توجد بيانات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp)) }
            } else {
                items(state.invoices, key = { it.id }) { inv ->
                    SalesInvoiceRow(inv, onClick = { onOpenVoucher(inv.id) })
                }
            }
        }
    }
}

@Composable
private fun SalesInvoiceRow(inv: InvoiceEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val accent = when (inv.type) {
                "SALE" -> Fv.Blue
                "RETURN" -> Fv.Red
                else -> Fv.Amber
            }
            val typeLabel = when (inv.type) {
                "SALE" -> "بيع"
                "RETURN" -> "مرتجع"
                else -> "طلب"
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(inv.number, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(inv.createdAt.toDateString(), color = Fv.TextMid, fontSize = 11.sp)
            }
            Text(typeLabel, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp))
            Text(inv.total.formatJod(AppLanguage.AR), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
