package com.jehadalomour.flowvan.screens.sale

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jehadalomour.flowvan.screens.AppBackHandler
import com.jehadalomour.flowvan.screens.components.CartItemCard
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.ProductAvatar
import com.jehadalomour.flowvan.screens.components.fvFieldColors
import com.jehadalomour.flowvan.screens.components.standardUnits
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.domain.model.ProductUnit
import com.jehadalomour.flowvan.shared.presentation.feature.sale.DiscountType
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherEvent
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherState
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SaleVoucherScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: SaleVoucherViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    var dialogProduct by remember { mutableStateOf<Product?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val displayProducts = remember(state.visibleProducts, selectedCategory) {
        val cat = selectedCategory
        if (cat == null) state.visibleProducts
        else state.visibleProducts.filter { it.category == cat }
    }

    LaunchedEffect(state.savedNumber) {
        if (state.savedNumber != null) onBack()
    }

    AppBackHandler(enabled = state.view == VoucherView.CART) {
        viewModel.onEvent(SaleVoucherEvent.ToggleView)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar — fixed, never scrolls ────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Fv.Surface,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (state.view == VoucherView.CART) viewModel.onEvent(SaleVoucherEvent.ToggleView)
                        else onBack()
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = null,
                            tint = Fv.TextHigh,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فاتورة بيع", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        state.customer?.let { Text(it.nameAr, color = Fv.TextMid, fontSize = 11.sp) }
                    }
                    CartToggle(state.view, state.cart.size) { viewModel.onEvent(SaleVoucherEvent.ToggleView) }
                }
            }

            // ── View content ─────────────────────────────────────────────────
            when (state.view) {
                VoucherView.PICKER -> {
                    ProductListPicker(
                        products = displayProducts,
                        allProducts = state.products,
                        productUnits = state.productUnits,
                        searchQuery = state.searchQuery,
                        selectedCategory = selectedCategory,
                        onSearch = { viewModel.onEvent(SaleVoucherEvent.SearchChanged(it)) },
                        onSelectCategory = { selectedCategory = it },
                        onTapProduct = { dialogProduct = it },
                        modifier = Modifier.weight(1f),
                    )
                    if (state.cart.isNotEmpty()) {
                        PickerSummaryBar(itemCount = state.cart.size, total = state.total)
                    }
                }
                VoucherView.CART -> {
                    CartView(
                        state = state,
                        onTapLine = { productId ->
                            dialogProduct = state.products.firstOrNull { it.id == productId }
                        },
                        onNotesChange = { viewModel.onEvent(SaleVoucherEvent.NotesChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    CartSummaryCard(
                        state = state,
                        onDiscountInputChange = { viewModel.onEvent(SaleVoucherEvent.VoucherDiscountInputChanged(it)) },
                        onDiscountTypeToggle = { viewModel.onEvent(SaleVoucherEvent.VoucherDiscountTypeToggled) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 6.dp),
                    )
                    val canSave = state.cart.isNotEmpty() && !state.isSaving
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (canSave)
                                        Modifier.background(Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))))
                                    else Modifier.background(Fv.SurfaceTop)
                                )
                                .clickable(enabled = canSave) { viewModel.onEvent(SaleVoucherEvent.OpenSaveSheet) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "حفظ الفاتورة",
                                color = if (canSave) Color.White else Fv.TextMid,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    dialogProduct?.let { product ->
        val currentLine = state.cart.firstOrNull { it.productId == product.id }
        AddItemBottomSheet(
            product = product,
            currentLine = currentLine,
            dbUnits = state.productUnits[product.id] ?: emptyList(),
            onConfirm = { qty, unit, unitPrice, unitConversionQty, discountPct ->
                viewModel.onEvent(
                    SaleVoucherEvent.ConfirmItemDialog(product, qty, unit, unitPrice, unitConversionQty, discountPct)
                )
                dialogProduct = null
            },
            onDelete = if (currentLine != null) {
                {
                    viewModel.onEvent(SaleVoucherEvent.RemoveLine(currentLine.productId))
                    dialogProduct = null
                }
            } else null,
            onDismiss = { dialogProduct = null },
        )
    }

    if (state.showSaveSheet) {
        PaymentMethodDialog(
            current = state.paymentMethod,
            onSelect = { viewModel.onEvent(SaleVoucherEvent.PaymentMethodSelected(it)) },
            onConfirm = { viewModel.onEvent(SaleVoucherEvent.ConfirmSave) },
            onDismiss = { viewModel.onEvent(SaleVoucherEvent.DismissSaveSheet) },
        )
    }

    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SaleVoucherEvent.DismissError) },
            title = { Text("خطأ", color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SaleVoucherEvent.DismissError) }) {
                    Text("حسناً", color = Fv.Blue)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

// ── Cart Toggle — blue icon button with red count badge ───────────────────────

@Composable
private fun CartToggle(view: VoucherView, count: Int, onClick: () -> Unit) {
    val isPickerView = view == VoucherView.PICKER
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPickerView) Fv.Blue else Fv.SurfaceTop)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPickerView) Res.drawable.ic_cart else Res.drawable.ic_inventory),
                contentDescription = null,
                tint = if (isPickerView) Color.White else Fv.TextMid,
                modifier = Modifier.size(21.dp),
            )
        }
        if (isPickerView && count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(19.dp)
                    .background(Fv.Red, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.coerceAtMost(99).toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Product List Picker ───────────────────────────────────────────────────────

@Composable
private fun ProductListPicker(
    products: List<Product>,
    allProducts: List<Product>,
    productUnits: Map<String, List<ProductUnit>>,
    searchQuery: String,
    selectedCategory: String?,
    onSearch: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onTapProduct: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = remember(allProducts) {
        allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("ابحث عن منتج...", color = Fv.TextMid) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = fvFieldColors(),
        )

        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryPill("الكل", selectedCategory == null) { onSelectCategory(null) }
                categories.forEach { cat ->
                    CategoryPill(cat, selectedCategory == cat) { onSelectCategory(cat) }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(products, key = { it.id }) { product ->
                val baseUnit = remember(product.id, productUnits) {
                    productUnits[product.id]?.minByOrNull { it.conversionQty }
                }
                ProductListCard(
                    product = product,
                    unitName = baseUnit?.name ?: product.unit,
                    unitPrice = baseUnit?.price ?: product.salePrice,
                    onTap = { onTapProduct(product) },
                )
            }
            if (products.isEmpty()) {
                item {
                    Text(
                        "لا توجد منتجات",
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
private fun ProductListCard(
    product: Product,
    unitName: String,
    unitPrice: Double,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductAvatar(
                seed = product.category,
                letter = product.nameAr.firstOrNull()?.toString() ?: "؟",
                size = 54.dp,
            )
            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.nameAr,
                    color = Fv.TextHigh,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(product.sku, color = Fv.TextMid, fontSize = 11.sp)
                    StockBadge(product)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        unitPrice.formatJod(AppLanguage.AR),
                        color = Fv.Blue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("/ $unitName", color = Fv.TextMid, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.size(8.dp))
            Text("‹", color = Fv.TextMid, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun StockBadge(product: Product) {
    val outOfStock = product.vanStock <= 0
    val low = !outOfStock && product.vanStock < product.minStock
    val chipColor = if (outOfStock) Fv.Red else if (low) Fv.Amber else Fv.Green
    val chipLabel = when {
        outOfStock -> "نافد"
        low -> "منخفض · ${product.vanStock}"
        else -> "متوفر · ${product.vanStock}"
    }
    Box(
        modifier = Modifier
            .background(chipColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(chipLabel, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CategoryPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Fv.Blue else Fv.SurfaceTop)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PickerSummaryBar(itemCount: Int, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2A3A))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            total.formatJod(AppLanguage.AR),
            color = Fv.Green,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "$itemCount ${if (itemCount == 1) "صنف" else "أصناف"}",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
    }
}

// ── Cart View ─────────────────────────────────────────────────────────────────

@Composable
private fun CartView(
    state: SaleVoucherState,
    onTapLine: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
    ) {
        items(state.cart, key = { it.productId }) { line ->
            CartItemCard(
                line = line,
                onTap = { onTapLine(line.productId) },
            )
        }
        if (state.cart.isEmpty()) {
            item {
                Text(
                    "السلة فارغة — أضف منتجات من قسم المنتجات",
                    color = Fv.TextMid,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("ملاحظات (اختياري)", color = Fv.TextMid, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
        }
    }
}

// ── Cart Summary Card ─────────────────────────────────────────────────────────

@Composable
private fun CartSummaryCard(
    state: SaleVoucherState,
    onDiscountInputChange: (String) -> Unit,
    onDiscountTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, Fv.Border),
    ) {
        Column {
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
                    SummaryDetailRow("المجموع الفرعي", state.subtotal.formatJod(AppLanguage.AR), Fv.TextMid)
                    if (state.lineDiscountTotal > 0)
                        SummaryDetailRow("خصم السطور", "- ${state.lineDiscountTotal.formatJod(AppLanguage.AR)}", Fv.Red)
                    Spacer(Modifier.height(8.dp))
                    VoucherDiscountSection(
                        type = state.voucherDiscountType,
                        input = state.voucherDiscountInput,
                        computedAmount = state.voucherDiscountAmount,
                        onInputChange = onDiscountInputChange,
                        onTypeToggle = onDiscountTypeToggle,
                    )
                    if (state.taxAmount > 0) {
                        Spacer(Modifier.height(6.dp))
                        SummaryDetailRow("الضريبة", state.taxAmount.formatJod(AppLanguage.AR), Fv.TextMid)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
                }
            }
            // Always-visible total row + expand/collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("الإجمالي النهائي", color = Fv.TextMid, fontSize = 12.sp)
                    Text(
                        state.total.formatJod(AppLanguage.AR),
                        color = Fv.Blue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(if (expanded) "▲" else "▼", color = Fv.TextMid, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SummaryDetailRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Voucher Discount Section ──────────────────────────────────────────────────

@Composable
private fun VoucherDiscountSection(
    type: DiscountType,
    input: String,
    computedAmount: Double,
    onInputChange: (String) -> Unit,
    onTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Fv.SurfaceTop, RoundedCornerShape(20.dp))
                .padding(2.dp),
        ) {
            listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to "قيمة").forEach { (t, label) ->
                val active = t == type
                Box(
                    modifier = Modifier
                        .clickable { if (!active) onTypeToggle() }
                        .background(if (active) Fv.Blue else Color.Transparent, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else Fv.TextMid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { v -> onInputChange(v.filter { it.isDigit() || it == '.' }.take(8)) },
            placeholder = {
                Text(
                    if (type == DiscountType.PERCENT) "خصم الفاتورة %" else "خصم الفاتورة د.أ",
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = {
                Text(
                    if (type == DiscountType.PERCENT) "%" else "د.أ",
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                )
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = fvFieldColors(),
        )

        if (computedAmount > 0) {
            Text(
                "- ${computedAmount.formatJod(AppLanguage.AR)}",
                color = Fv.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ── Add Item — Bottom Sheet ───────────────────────────────────────────────────

@Composable
private fun AddItemBottomSheet(
    product: Product,
    currentLine: CartLine?,
    dbUnits: List<ProductUnit>,
    onConfirm: (qty: Double, unit: String, unitPrice: Double, unitConversionQty: Double, discountPct: Double) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val effectiveUnits: List<ProductUnit> = remember(product.id, dbUnits) {
        if (dbUnits.isNotEmpty()) dbUnits
        else (listOf(product.unit) + standardUnits)
            .filter { it.isNotBlank() }.distinct()
            .map { name ->
                ProductUnit(id = name, productId = product.id, name = name, price = product.salePrice, conversionQty = 1.0)
            }
    }
    val initialUnit: ProductUnit = remember(product.id, currentLine) {
        val cartUnitName = currentLine?.unit?.takeIf { it.isNotBlank() }
        effectiveUnits.firstOrNull { it.name == cartUnitName }
            ?: effectiveUnits.firstOrNull()
            ?: ProductUnit(id = product.unit, productId = product.id, name = product.unit, price = product.salePrice, conversionQty = 1.0)
    }

    var qty by remember(product.id) { mutableStateOf(currentLine?.qty ?: 1.0) }
    var selectedUnit by remember(product.id) { mutableStateOf(initialUnit) }
    var lineDiscountType by remember(product.id) { mutableStateOf(DiscountType.PERCENT) }
    var discountText by remember(product.id) {
        val initial = currentLine?.discountPct ?: 0.0
        mutableStateOf(if (initial > 0) (initial * 100).toInt().toString() else "")
    }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val gross = selectedUnit.price * qty
    val discountPct = when (lineDiscountType) {
        DiscountType.PERCENT -> discountText.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0) ?: 0.0
        DiscountType.VALUE -> if (gross > 0) (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, gross) / gross else 0.0
    }
    val lineTotal = gross * (1.0 - discountPct)

    val blueGradient = Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C)))
    val greenGradient = Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56)))
    val heroGradient = Brush.linearGradient(listOf(Color(0xFFEEF4FF), Color(0xFFE6F1FB), Color(0xFFE1F5EE)))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Full-screen container — tap above sheet to dismiss
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // ── Sheet ──────────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column {
                    // Drag handle
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color(0xFFDDE8F5), RoundedCornerShape(2.dp)),
                        )
                    }

                    // ── Hero ──
                    Box(modifier = Modifier.fillMaxWidth().background(heroGradient)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(blueGradient),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        product.nameAr.firstOrNull()?.toString() ?: "؟",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                product.nameAr,
                                color = Color(0xFF1A2A3A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(blueGradient)
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "${selectedUnit.price.formatJod(AppLanguage.AR)} / ${selectedUnit.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        // SKU badge — top visual-left in RTL
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.88f))
                                .border(0.5.dp, Color(0xFFB5D4F4), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(product.sku, color = Color(0xFF185FA5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // ── Body ──
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Quantity row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("العدد المطلوب", color = Color(0xFF5A7399), fontSize = 14.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White, CircleShape)
                                        .border(0.5.dp, Color(0xFFC8D8EC), CircleShape)
                                        .clickable(enabled = qty > 1) { qty -= 1 },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("−", color = Color(0xFF185FA5), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    qty.toInt().toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A2A3A),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(blueGradient)
                                        .clickable { qty += 1 },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Unit dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(14.dp))
                                    .clickable { unitDropdownExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▾", color = Color(0xFF185FA5), fontSize = 18.sp)
                                Text(
                                    unitLabel(selectedUnit),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A2A3A),
                                )
                            }
                            DropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false },
                            ) {
                                effectiveUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unitLabel(unit), fontWeight = FontWeight.SemiBold) },
                                        onClick = { selectedUnit = unit; unitDropdownExpanded = false },
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDE8F5)))

                        // Unit price
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                selectedUnit.price.formatJod(AppLanguage.AR),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF185FA5),
                            )
                            Text("· سعر الوحدة", fontSize = 13.sp, color = Color(0xFF5A7399))
                        }

                        // Line discount
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(10.dp)),
                                ) {
                                    listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to "قيمة").forEach { (t, label) ->
                                        val active = t == lineDiscountType
                                        Box(
                                            modifier = Modifier
                                                .clickable { if (!active) { lineDiscountType = t; discountText = "" } }
                                                .then(if (active) Modifier.background(blueGradient) else Modifier)
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                label,
                                                color = if (active) Color.White else Color(0xFF8A9AB0),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                                Text("خصم السطر", color = Color(0xFF8A9AB0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { v -> discountText = v.filter { it.isDigit() || it == '.' }.take(8) },
                                placeholder = { Text("0", color = Color(0xFFB0BEC5)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = {
                                    Text(
                                        if (lineDiscountType == DiscountType.PERCENT) "%" else "د.أ",
                                        color = Color(0xFF185FA5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fvFieldColors(),
                            )
                        }

                        // Total card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(blueGradient)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                lineTotal.formatJod(AppLanguage.AR),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text("إجمالي السعر", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }

                    // ── Actions ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (onDelete != null) {
                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFF0F0))
                                    .border(0.5.dp, Color(0xFFF7C1C1), RoundedCornerShape(16.dp))
                                    .clickable { onDelete() }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("حذف", color = Color(0xFFE24B4A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(16.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("إلغاء", color = Color(0xFF5A7399), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(if (onDelete != null) 1.8f else 2f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (qty > 0) Modifier.background(greenGradient)
                                    else Modifier.background(Fv.SurfaceTop)
                                )
                                .clickable(enabled = qty > 0) {
                                    onConfirm(qty, selectedUnit.name, selectedUnit.price, selectedUnit.conversionQty, discountPct)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_cart),
                                    contentDescription = null,
                                    tint = if (qty > 0) Color.White else Fv.TextMid,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    if (currentLine == null) "إضافة للسلة" else "تحديث السطر",
                                    color = if (qty > 0) Color.White else Fv.TextMid,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun unitLabel(unit: ProductUnit): String {
    val qty = if (unit.conversionQty % 1.0 == 0.0) unit.conversionQty.toInt().toString() else unit.conversionQty.toString()
    return if (unit.conversionQty == 1.0) unit.name else "${unit.name}  ×$qty"
}

// ── Payment Method Dialog ─────────────────────────────────────────────────────

@Composable
private fun PaymentMethodDialog(
    current: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("طريقة الدفع", color = Fv.TextHigh) },
        text = {
            Column {
                listOf(
                    PaymentMethod.CASH to "نقداً",
                    PaymentMethod.CREDIT to "ذمم",
                ).forEach { (method, label) ->
                    val active = method == current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(method) }
                            .background(if (active) Fv.Blue else Fv.SurfaceHigh, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            label,
                            color = if (active) Color.White else Fv.TextMid,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("تأكيد", color = Fv.Green) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = Fv.TextMid) } },
        containerColor = Fv.Surface,
    )
}
