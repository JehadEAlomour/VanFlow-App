package com.jehadalomour.flowvan.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage

val standardUnits = listOf("حبة", "علبة", "كرتونة", "طرد", "كيس", "دزينة")

@Composable
fun ProductPickerColumn(
    products: List<Product>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onAdd: (Product) -> Unit,
    showStockBadge: Boolean,
    modifier: Modifier = Modifier,
    cartQtyMap: Map<String, Double> = emptyMap(),
    onStep: ((Product, Int) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text(stringResource(Res.string.sale_search_product), color = Fv.TextMid) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fvFieldColors(),
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(products, key = { it.id }) { product ->
                ProductRow(
                    product = product,
                    showStockBadge = showStockBadge,
                    currentQty = cartQtyMap[product.id] ?: 0.0,
                    onTap = { onAdd(product) },
                    onStep = onStep?.let { cb -> { delta -> cb(product, delta) } },
                )
            }
            if (products.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.cart_no_products),
                        color = Fv.TextMid,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    showStockBadge: Boolean,
    currentQty: Double,
    onTap: () -> Unit,
    onStep: ((Int) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductAvatar(seed = product.category, letter = product.nameAr.firstOrNull()?.toString() ?: "؟", size = 46.dp)
            Spacer(Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.nameAr,
                    color = Fv.TextHigh,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${product.sku} · ${product.salePrice.formatJod(AppLanguage.AR)}",
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                )
                if (showStockBadge) {
                    Spacer(Modifier.height(4.dp))
                    val outOfStock = product.vanStock <= 0
                    val low = !outOfStock && product.vanStock < product.minStock
                    val chipColor = if (outOfStock) Fv.Red else if (low) Fv.Amber else Fv.Green
                    val chipLabel = when {
                        outOfStock -> stringResource(Res.string.van_stock_out_of_stock)
                        low -> stringResource(Res.string.cart_stock_low, product.vanStock)
                        else -> stringResource(Res.string.cart_stock_available, product.vanStock)
                    }
                    Box(
                        modifier = Modifier
                            .background(chipColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(chipLabel, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.size(8.dp))

            if (onStep != null) {
                InlineQtyStepper(qty = currentQty, onStep = onStep)
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Fv.Blue, CircleShape)
                        .clickable(onClick = onTap),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InlineQtyStepper(qty: Double, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", enabled = qty > 0) { onStep(-1) }
        Text(
            text = qty.toInt().toString(),
            color = if (qty > 0) Fv.Blue else Fv.TextMid,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp).padding(horizontal = 4.dp),
        )
        StepperButton("+") { onStep(+1) }
    }
}

@Composable
fun CartLineRow(
    line: CartLine,
    onChangeQty: (Double) -> Unit,
    onRemove: () -> Unit,
    onTap: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().let { if (onTap != null) it.clickable(onClick = onTap) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductAvatar(seed = line.nameAr, letter = line.nameAr.firstOrNull()?.toString() ?: "؟", size = 40.dp)
            Spacer(Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line.nameAr,
                    color = Fv.TextHigh,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val unitStr = line.unit.takeIf { it.isNotBlank() } ?: ""
                Text(
                    buildString {
                        append(line.sku)
                        if (unitStr.isNotBlank()) append(" · $unitStr")
                        append(" · ${line.unitPrice.formatJod(AppLanguage.AR)}")
                    },
                    color = Fv.TextMid,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        line.lineTotal.formatJod(AppLanguage.AR),
                        color = Fv.TextHigh,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (line.discountPct > 0) {
                        Box(
                            modifier = Modifier
                                .background(Fv.Red.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                stringResource(Res.string.cart_discount_pct, (line.discountPct * 100).toInt()),
                                color = Fv.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(6.dp))
            QtyStepper(line.qty, onChangeQty)
            Spacer(Modifier.size(2.dp))

            Box(
                modifier = Modifier.clickable(onClick = onRemove).padding(6.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cancel),
                    contentDescription = null,
                    tint = Fv.Red,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun CartItemCard(line: CartLine, onTap: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, Color(0xFFDDE8F5)),
    ) {
        Column {
            // Top section: avatar + name/sku + edit badge
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductAvatar(
                    seed = line.nameAr,
                    letter = line.nameAr.firstOrNull()?.toString() ?: "؟",
                    size = 50.dp,
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        line.nameAr,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val unitStr = line.unit.takeIf { it.isNotBlank() } ?: ""
                    Text(
                        buildString {
                            append(line.sku)
                            if (unitStr.isNotBlank()) append(" · $unitStr")
                        },
                        color = Fv.TextMid,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE6F1FB))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(stringResource(Res.string.edit), color = Fv.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFDDE8F5)))
            // Bottom section: price info + qty pill
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${line.unitPrice.formatJod(AppLanguage.AR)} × ${line.qty.toInt()}",
                        color = Fv.TextMid,
                        fontSize = 11.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            line.lineTotal.formatJod(AppLanguage.AR),
                            color = Fv.Blue,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (line.discountPct > 0) {
                            Text(
                                stringResource(Res.string.cart_discount_pct, (line.discountPct * 100).toInt()),
                                color = Fv.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Fv.SurfaceTop, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        "× ${line.qty.toInt()}",
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QtyStepper(qty: Double, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", enabled = qty > 0) { if (qty > 0) onChange(qty - 1) }
        Text(
            qty.toInt().toString(),
            color = Fv.TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 22.dp).padding(horizontal = 6.dp),
        )
        StepperButton("+") { onChange(qty + 1) }
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(if (enabled) Fv.SurfaceTop else Fv.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Fv.TextHigh else Fv.TextMid, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TotalsStrip(
    subtotal: Double,
    discount: Double? = null,
    lineDiscount: Double? = null,
    voucherDiscount: Double? = null,
    tax: Double,
    total: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.SurfaceHigh),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            TotalsRow(stringResource(Res.string.voucher_detail_subtotal), subtotal.formatJod(AppLanguage.AR), Fv.TextMid)
            // Split display when called with separate line/voucher discounts
            if (lineDiscount != null || voucherDiscount != null) {
                if (lineDiscount != null && lineDiscount > 0)
                    TotalsRow(stringResource(Res.string.cart_line_discount), "- ${lineDiscount.formatJod(AppLanguage.AR)}", Fv.Red)
                if (voucherDiscount != null && voucherDiscount > 0)
                    TotalsRow(stringResource(Res.string.cart_voucher_discount), "- ${voucherDiscount.formatJod(AppLanguage.AR)}", Fv.Red)
            } else if (discount != null && discount > 0) {
                TotalsRow(stringResource(Res.string.voucher_detail_discount), "- ${discount.formatJod(AppLanguage.AR)}", Fv.Red)
            }
            if (tax > 0) {
                TotalsRow(stringResource(Res.string.voucher_detail_tax), tax.formatJod(AppLanguage.AR), Fv.TextMid)
            }
            Spacer(Modifier.height(6.dp))
            TotalsRow(stringResource(Res.string.voucher_detail_total), total.formatJod(AppLanguage.AR), Fv.Green, bold = true, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    value: String,
    color: Color,
    bold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    Row {
        Text(label, color = Fv.TextMid, fontSize = fontSize, modifier = Modifier.weight(1f))
        Text(
            value,
            color = color,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun ProductAvatar(seed: String, letter: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(avatarColorFor(seed), RoundedCornerShape((size.value * 0.25f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun avatarColorFor(seed: String): Color {
    val palette = listOf(
        Color(0xFF4B8FF6),
        Color(0xFF34C759),
        Color(0xFFFF9500),
        Color(0xFF5856D6),
        Color(0xFF00C7BE),
        Color(0xFFAF52DE),
        Color(0xFFFF3B30),
    )
    return palette[kotlin.math.abs(seed.hashCode()) % palette.size]
}

@Composable
fun fvFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Fv.TextHigh,
    unfocusedTextColor = Fv.TextHigh,
    focusedContainerColor = Fv.Surface,
    unfocusedContainerColor = Fv.Surface,
    focusedIndicatorColor = Fv.Blue,
    unfocusedIndicatorColor = Fv.Border,
    cursorColor = Fv.Blue,
    focusedPlaceholderColor = Fv.TextMid,
    unfocusedPlaceholderColor = Fv.TextMid,
)
