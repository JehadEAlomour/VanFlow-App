package com.jehadalomour.flowvan.screens.vanstock

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
import androidx.compose.material3.OutlinedTextField
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
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.darkFieldColors
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.presentation.feature.vanstock.StockStatus
import com.jehadalomour.flowvan.shared.presentation.feature.vanstock.VanStockEvent
import com.jehadalomour.flowvan.shared.presentation.feature.vanstock.VanStockViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.vanstock.stockStatus
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VanStockScreen(
    onBack: () -> Unit,
    viewModel: VanStockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text("مخزون الفان", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Fv.Surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    StatCell("الأصناف", state.allProducts.size.toString())
                    StatCell("القيمة الإجمالية", state.totalInventoryValue.formatJod(AppLanguage.AR))
                    StatCell("منخفض المخزون", state.allProducts.count { it.vanStock < it.minStock }.toString())
                }
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(VanStockEvent.SearchChanged(it)) },
                placeholder = { Text("بحث (اسم، SKU، فئة...)", color = Fv.TextMid, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = darkFieldColors(),
                singleLine = true,
            )

            if (state.categories.size > 1) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    item {
                        CategoryChip("الكل", state.selectedCategory == null) {
                            viewModel.onEvent(VanStockEvent.CategorySelected(null))
                        }
                    }
                    items(state.categories) { cat ->
                        CategoryChip(cat, state.selectedCategory == cat) {
                            viewModel.onEvent(VanStockEvent.CategorySelected(cat))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.visibleProducts, key = { it.id }) { product ->
                    ProductCard(product, state.nowMs)
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Fv.TextMid, fontSize = 10.sp)
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(if (selected) Fv.Blue else Fv.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (selected) Fv.TextHigh else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ProductCard(product: Product, nowMs: Long) {
    val status = product.stockStatus(nowMs)
    val (statusLabel, statusColor) = when (status) {
        StockStatus.OUT -> "نفد ❌" to Fv.Red
        StockStatus.LOW -> "⚠️ منخفض" to Fv.Amber
        StockStatus.EXPIRING -> "⏰ ينتهي قريباً" to Fv.Amber
        StockStatus.GOOD -> "متوفر ✓" to Fv.Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status == StockStatus.OUT || status == StockStatus.LOW)
                Fv.Surface else Fv.Surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(product.nameEn, color = Fv.TextMid, fontSize = 11.sp)
                Text("SKU: ${product.sku} · ${product.category}", color = Fv.TextMid, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${product.vanStock} / ${product.minStock}",
                    color = if (status != StockStatus.GOOD) statusColor else Fv.TextHigh,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("كمية / حد", color = Fv.TextMid, fontSize = 9.sp)
                Text(product.salePrice.formatJod(AppLanguage.AR), color = Fv.TextMid, fontSize = 11.sp)
            }
        }
    }
}
