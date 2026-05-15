package com.jehadalomour.flowvan.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage

@Composable
fun ProductPickerColumn(
    products: List<Product>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onAdd: (Product) -> Unit,
    showStockBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("ابحث عن منتج", color = Fv.TextMid) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fvFieldColors(),
            )
        }
        items(products, key = { it.id }) { product ->
            ProductRow(product, showStockBadge, onAdd = { onAdd(product) })
        }
        if (products.isEmpty()) {
            item {
                Text(
                    "لا منتجات",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, showStockBadge: Boolean, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${product.sku} · ${product.category}", color = Fv.TextMid, fontSize = 10.sp)
                if (showStockBadge) {
                    Spacer(Modifier.height(4.dp))
                    val low = product.vanStock < product.minStock
                    val outOfStock = product.vanStock <= 0
                    Text(
                        text = "المخزون: ${product.vanStock}" + if (outOfStock) " · نفد" else if (low) " · منخفض" else "",
                        color = if (outOfStock) Fv.Red else if (low) Fv.Amber else Fv.TextMid,
                        fontSize = 10.sp,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(product.salePrice.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Fv.Blue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CartLineRow(
    line: CartLine,
    onChangeQty: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.nameAr, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(line.sku, color = Fv.TextMid, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${line.qty.toInt()} × ${line.unitPrice.formatJod(AppLanguage.AR)} = ${line.lineTotal.formatJod(AppLanguage.AR)}",
                    color = Fv.TextMid,
                    fontSize = 10.sp,
                )
            }
            QtyStepper(line.qty, onChangeQty)
            Spacer(androidx.compose.ui.Modifier)
            Box(
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cancel),
                    contentDescription = null,
                    tint = Fv.Red,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun QtyStepper(qty: Double, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−") { if (qty > 0) onChange(qty - 1) }
        Text(
            qty.toInt().toString(),
            color = Fv.TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        StepperButton("+") { onChange(qty + 1) }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(Fv.SurfaceTop, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun TotalsStrip(
    subtotal: Double,
    discount: Double?,
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
            TotalsRow("المجموع الفرعي", subtotal.formatJod(AppLanguage.AR), Fv.TextMid)
            if (discount != null && discount > 0) TotalsRow("الخصم", "-${discount.formatJod(AppLanguage.AR)}", Fv.Red)
            TotalsRow("الضريبة (16%)", tax.formatJod(AppLanguage.AR), Fv.TextMid)
            Spacer(Modifier.height(6.dp))
            TotalsRow("الإجمالي", total.formatJod(AppLanguage.AR), Fv.Green, bold = true, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
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
