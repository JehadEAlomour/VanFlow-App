package com.jehadalomour.flowvan.screens.reports

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.jehadalomour.flowvan.shared.presentation.feature.reports.ItemSalesRow
import com.jehadalomour.flowvan.shared.presentation.feature.reports.ItemsSalesReportViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun ItemsSalesReportScreen(
    onBack: () -> Unit,
    viewModel: ItemsSalesReportViewModel = koinViewModel(),
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
                    Text("مبيعات الأصناف", color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                DateRangeBar(state.from, state.to) { f, t ->
                    viewModel.setFrom(f); viewModel.setTo(t)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryPill(
                        "إجمالي المبيعات",
                        state.grandTotalAmount.formatJod(AppLanguage.AR),
                        Fv.Blue,
                        Modifier.weight(1f),
                    )
                    SummaryPill(
                        "إجمالي الكمية",
                        "${state.grandTotalQty.roundToInt()} وحدة",
                        Fv.Purple,
                        Modifier.weight(1f),
                    )
                }
            }
            if (state.items.isEmpty()) {
                item {
                    Text("لا توجد مبيعات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                }
            } else {
                item {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text("#", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                        Text("الصنف", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("الكمية", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                        Text("الإجمالي", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                    }
                }
                itemsIndexed(state.items, key = { _, item -> item.productId }) { index, item ->
                    ItemSalesRowCard(rank = index + 1, item = item, grandTotal = state.grandTotalAmount)
                }
            }
        }
    }
}

@Composable
private fun ItemSalesRowCard(rank: Int, item: ItemSalesRow, grandTotal: Double) {
    val share = if (grandTotal > 0) (item.totalAmount / grandTotal).toFloat() else 0f
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(
                        when {
                            rank == 1 -> Fv.Amber.copy(alpha = 0.3f)
                            rank == 2 -> Fv.TextMid.copy(alpha = 0.2f)
                            rank == 3 -> Fv.Blue.copy(alpha = 0.2f)
                            else -> Fv.SurfaceHigh
                        },
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$rank", color = Fv.TextHigh, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.sku, color = Fv.TextMid, fontSize = 10.sp)
                }
                Text(
                    "${item.totalQty.roundToInt()}",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                    modifier = Modifier.width(50.dp),
                )
                Text(
                    item.totalAmount.formatJod(AppLanguage.AR),
                    color = Fv.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(80.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(Fv.SurfaceHigh, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(Fv.Blue, CircleShape),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${(share * 100).roundToInt()}%", color = Fv.TextMid, fontSize = 10.sp)
            }
        }
    }
}
