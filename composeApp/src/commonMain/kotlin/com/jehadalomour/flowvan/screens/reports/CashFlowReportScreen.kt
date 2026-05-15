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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.presentation.feature.reports.CashEntry
import com.jehadalomour.flowvan.shared.presentation.feature.reports.CashFlowReportViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CashFlowReportScreen(
    onBack: () -> Unit,
    onOpenVoucher: (String) -> Unit,
    onOpenReceipt: (String) -> Unit,
    viewModel: CashFlowReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                    Spacer(Modifier.width(4.dp))
                    Text("الكشف اليومي", color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill("مبيعات", state.salesTotal.formatJod(AppLanguage.AR), Fv.Blue, Modifier.weight(1f))
                            SummaryPill("مرتجعات", state.returnsTotal.formatJod(AppLanguage.AR), Fv.Red, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill("تحصيلات", state.collectionsTotal.formatJod(AppLanguage.AR), Fv.Green, Modifier.weight(1f))
                            SummaryPill("صافي النقد", state.netCash.formatJod(AppLanguage.AR),
                                if (state.netCash >= 0) Fv.Amber else Fv.Red, Modifier.weight(1f))
                        }
                    }
                }
            }
            if (state.entries.isEmpty()) {
                item { Text("لا توجد حركات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp)) }
            } else {
                items(state.entries, key = { entry ->
                    when (entry) {
                        is CashEntry.Sale -> "sale-${entry.invoice.id}"
                        is CashEntry.Return -> "return-${entry.invoice.id}"
                        is CashEntry.Collection -> "col-${entry.payment.id}"
                    }
                }) { entry ->
                    CashEntryRow(
                        entry = entry,
                        onClick = {
                            when (entry) {
                                is CashEntry.Sale -> onOpenVoucher(entry.invoice.id)
                                is CashEntry.Return -> onOpenVoucher(entry.invoice.id)
                                is CashEntry.Collection -> onOpenReceipt(entry.payment.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CashEntryRow(entry: CashEntry, onClick: () -> Unit) {
    val emoji: String
    val label: String
    val ref: String
    val amount: Double
    val color: androidx.compose.ui.graphics.Color
    when (entry) {
        is CashEntry.Sale -> { emoji = "🧾"; label = "بيع"; ref = entry.invoice.number; amount = entry.invoice.total; color = Fv.Blue }
        is CashEntry.Return -> { emoji = "↩️"; label = "مرتجع"; ref = entry.invoice.number; amount = entry.invoice.total; color = Fv.Red }
        is CashEntry.Collection -> { emoji = "💵"; label = "تحصيل"; ref = entry.payment.number; amount = entry.payment.amount; color = Fv.Green }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ref, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(entry.timestampMs.toDateTimeString(), color = Fv.TextMid, fontSize = 10.sp)
            }
            Text(label, color = color, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
            Text(amount.formatJod(AppLanguage.AR), color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
