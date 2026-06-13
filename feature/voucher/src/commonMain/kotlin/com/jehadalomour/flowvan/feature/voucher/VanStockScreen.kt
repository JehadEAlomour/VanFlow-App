package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.feature.voucher.StockStatus
import com.jehadalomour.flowvan.feature.voucher.VanStockEvent
import com.jehadalomour.flowvan.feature.voucher.VanStockViewModel
import com.jehadalomour.flowvan.feature.voucher.stockStatus
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VanStockScreen(
    onBack: () -> Unit,
    viewModel: VanStockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Sticky Top Bar ────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Fv.Surface,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.SurfaceTop)
                        .border(0.5.dp, Fv.Border, RoundedCornerShape(11.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(Res.string.van_stock_title),
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C))))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_receipt),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(stringResource(Res.string.van_stock_print), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Scrollable Content ────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Fv.BgDeepest),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Stats Hero
            item {
                StatsHero(
                    totalItems = state.allProducts.size,
                    totalValue = state.totalInventoryValue,
                    lowStockCount = state.allProducts.count { it.vanStock < it.minStock },
                )
            }

            // Search
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(VanStockEvent.SearchChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.van_stock_search_full_hint), color = Fv.TextMid, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Fv.TextHigh,
                        unfocusedTextColor = Fv.TextHigh,
                        focusedContainerColor = Fv.Surface,
                        unfocusedContainerColor = Fv.Surface,
                        focusedIndicatorColor = Fv.Blue,
                        unfocusedIndicatorColor = Fv.Border,
                        cursorColor = Fv.Blue,
                        focusedPlaceholderColor = Fv.TextMid,
                        unfocusedPlaceholderColor = Fv.TextMid,
                    ),
                )
            }

            // Category pills
            if (state.categories.size > 1) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryPill(stringResource(Res.string.all), state.selectedCategory == null) {
                            viewModel.onEvent(VanStockEvent.CategorySelected(null))
                        }
                        state.categories.forEach { cat ->
                            CategoryPill(cat, state.selectedCategory == cat) {
                                viewModel.onEvent(
                                    VanStockEvent.CategorySelected(if (state.selectedCategory == cat) null else cat),
                                )
                            }
                        }
                    }
                }
            }

            // Stock cards
            items(state.visibleProducts, key = { it.id }) { product ->
                StockCard(product = product, nowMs = state.nowMs, onClick = { selectedProduct = product })
            }

            if (state.visibleProducts.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        stringResource(Res.string.van_stock_no_results),
                        color = Fv.TextMid,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    selectedProduct?.let { product ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            containerColor = Fv.Surface,
        ) {
            ProductDetailSheet(product = product, nowMs = state.nowMs)
        }
    }
}

// ── Stats Hero ────────────────────────────────────────────────────────────────

@Composable
private fun StatsHero(totalItems: Int, totalValue: Double, lowStockCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C)))),
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-30).dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(60.dp)),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ShStatColumn(value = "$totalItems", label = stringResource(Res.string.van_stock_stat_items), valueColor = Color.White)
            Box(modifier = Modifier.width(0.5.dp).height(48.dp).background(Color.White.copy(alpha = 0.2f)))
            ShStatColumn(
                value = totalValue.formatJod(AppLanguage.AR),
                label = stringResource(Res.string.van_stock_stat_total_value),
                valueColor = Color(0xFF6EE7B7),
            )
            Box(modifier = Modifier.width(0.5.dp).height(48.dp).background(Color.White.copy(alpha = 0.2f)))
            ShStatColumn(
                value = "$lowStockCount",
                label = stringResource(Res.string.van_stock_stat_low_stock),
                valueColor = if (lowStockCount > 0) Color(0xFFFCD34D) else Color.White,
            )
        }
    }
}

@Composable
private fun ShStatColumn(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
        Text(
            value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Category Pill ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Fv.Blue else Fv.Surface)
            .border(0.5.dp, if (active) Fv.Blue else Fv.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Stock Card ────────────────────────────────────────────────────────────────

@Composable
private fun StockCard(product: Product, nowMs: Long, onClick: () -> Unit) {
    val status = product.stockStatus(nowMs)
    val statusColor = when (status) {
        StockStatus.GOOD -> Fv.Green
        StockStatus.LOW, StockStatus.EXPIRING -> Fv.Amber
        StockStatus.OUT -> Fv.Red
    }
    val statusLabel = when (status) {
        StockStatus.GOOD -> stringResource(Res.string.van_stock_in_stock)
        StockStatus.LOW -> stringResource(Res.string.van_stock_low_stock)
        StockStatus.EXPIRING -> stringResource(Res.string.van_stock_expiring_soon)
        StockStatus.OUT -> stringResource(Res.string.van_stock_out_of_stock)
    }
    val statusIcon = when (status) {
        StockStatus.GOOD -> Res.drawable.ic_check
        StockStatus.EXPIRING -> Res.drawable.ic_alarm
        else -> Res.drawable.ic_warning
    }
    val qtyColor = when (status) {
        StockStatus.GOOD -> Fv.TextHigh
        StockStatus.LOW, StockStatus.EXPIRING -> Fv.Amber
        StockStatus.OUT -> Fv.Red
    }
    val progressBrush = when (status) {
        StockStatus.GOOD -> Brush.horizontalGradient(listOf(Color(0xFF1D9E75), Color(0xFF4ADE80)))
        StockStatus.LOW, StockStatus.EXPIRING -> Brush.horizontalGradient(listOf(Color(0xFFB36C00), Color(0xFFF59E0B)))
        StockStatus.OUT -> Brush.horizontalGradient(listOf(Color(0xFFD63B3B), Color(0xFFF87171)))
    }
    val stockRef = maxOf(product.vanStock, product.minStock * 3).toFloat().coerceAtLeast(1f)
    val progress = (product.vanStock.toFloat() / stockRef).coerceIn(0f, 1f)
    val pct = (progress * 100).toInt()
    val (avatarBg, avatarFg) = categoryAvatarStyle(product.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Fv.Surface)
            .border(0.5.dp, Fv.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            // ── Top Section ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(avatarBg)
                        .border(0.5.dp, Fv.Border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_inventory),
                        contentDescription = null,
                        tint = avatarFg,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Product info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.nameAr,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        product.nameEn,
                        color = Fv.TextLow,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${product.sku} · ${product.category}", color = Fv.TextLow, fontSize = 10.sp)
                }
                Spacer(Modifier.width(12.dp))
                // Right column: qty + badge + price
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${product.vanStock} / ${product.minStock}",
                        color = qtyColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(stringResource(Res.string.van_stock_qty_min_label), color = Fv.TextLow, fontSize = 9.sp)
                    // Status badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painterResource(statusIcon),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        product.salePrice.formatJod(AppLanguage.AR),
                        color = Fv.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // ── Progress Section ──────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.SurfaceTop))
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(Res.string.van_stock_level), color = Fv.TextMid, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Text("$pct%", color = Fv.TextMid, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Fv.SurfaceTop),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(progressBrush),
                    )
                }
            }
        }
    }
}

// ── Product Detail Sheet ──────────────────────────────────────────────────────

@Composable
private fun ProductDetailSheet(product: Product, nowMs: Long) {
    val status = product.stockStatus(nowMs)
    val statusColor = when (status) {
        StockStatus.GOOD -> Fv.Green
        StockStatus.LOW, StockStatus.EXPIRING -> Fv.Amber
        StockStatus.OUT -> Fv.Red
    }
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(product.nameAr, color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(product.nameEn, color = Fv.TextMid, fontSize = 13.sp)

        DetailRow("SKU", product.sku)
        DetailRow(stringResource(Res.string.van_stock_category), product.category)
        DetailRow(stringResource(Res.string.van_stock_unit), product.unit)
        DetailRow(stringResource(Res.string.van_stock_current_qty), "${product.vanStock}")
        DetailRow(stringResource(Res.string.van_stock_min), "${product.minStock}")
        DetailRow(stringResource(Res.string.van_stock_sale_price), product.salePrice.formatJod(AppLanguage.AR) + " " + stringResource(Res.string.currency_jod))
        DetailRow(stringResource(Res.string.van_stock_cost_price), product.costPrice.formatJod(AppLanguage.AR) + " " + stringResource(Res.string.currency_jod))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (status) {
                    StockStatus.GOOD -> stringResource(Res.string.van_stock_in_stock)
                    StockStatus.LOW -> stringResource(Res.string.van_stock_low_stock_full)
                    StockStatus.EXPIRING -> stringResource(Res.string.van_stock_expiring_soon)
                    StockStatus.OUT -> stringResource(Res.string.van_stock_out_of_stock_full)
                },
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Fv.TextMid, fontSize = 13.sp)
        Text(value, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Maps category name → (avatarBg, avatarFg) using a deterministic palette
private fun categoryAvatarStyle(category: String): Pair<Color, Color> {
    val palette = listOf(
        Color(0xFFFEF3E0) to Color(0xFFB36C00), // amber — Dry Goods
        Color(0xFFE6F1FB) to Color(0xFF185FA5), // blue  — Cleaning
        Color(0xFFE1F5EE) to Color(0xFF1D9E75), // green — Snacks
        Color(0xFFEEE9FB) to Color(0xFF7757D4), // purple — Beverages
        Color(0xFFFFEBEB) to Color(0xFFD63B3B), // red   — Canned
    )
    return palette[abs(category.hashCode()) % palette.size]
}
