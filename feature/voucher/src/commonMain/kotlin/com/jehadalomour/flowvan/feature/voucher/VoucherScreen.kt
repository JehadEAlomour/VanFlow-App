package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import com.jehadalomour.flowvan.feature.voucher.AppBackHandler
import com.jehadalomour.flowvan.core.designsystem.components.CartItemCard
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.ProductAvatar
import com.jehadalomour.flowvan.core.designsystem.components.fvFieldColors
import com.jehadalomour.flowvan.core.designsystem.components.standardUnits
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.feature.voucher.ReturnReason
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.feature.voucher.VoucherEvent
import com.jehadalomour.flowvan.feature.voucher.VoucherState
import com.jehadalomour.flowvan.feature.voucher.VoucherType
import com.jehadalomour.flowvan.feature.voucher.VoucherViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import kotlin.math.floor
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Save-button palette — only colors live in the UI layer ───────────────────
private val saleSaveGradient    = listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))
private val returnSaveGradient  = listOf(Color(0xFFD63B3B), Color(0xFF992828))
private val orderSaveGradient   = listOf(Color(0xFF0E9E91), Color(0xFF0A6E66))

@Composable
fun VoucherScreen(
    customerId: String,
    type: VoucherType,
    onBack: () -> Unit,
    onPrint: (invoiceId: String) -> Unit,
    viewModel: VoucherViewModel = koinViewModel { parametersOf(customerId, type) },
) {
    val state by viewModel.state.collectAsState()
    var dialogProduct by remember { mutableStateOf<Product?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val displayProducts = remember(state.visibleProducts, selectedCategory) {
        val cat = selectedCategory
        if (cat == null) state.visibleProducts else state.visibleProducts.filter { it.category == cat }
    }

    val saveGradient = when (state.type) {
        VoucherType.SALE -> saleSaveGradient
        VoucherType.RETURN -> returnSaveGradient
        VoucherType.ORDER -> orderSaveGradient
    }

    LaunchedEffect(state.savedId) {
        val id = state.savedId ?: return@LaunchedEffect
        onPrint(id)
    }

    AppBackHandler(enabled = state.view == VoucherView.CART) {
        viewModel.onEvent(VoucherEvent.ToggleView)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
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
                        if (state.view == VoucherView.CART) viewModel.onEvent(VoucherEvent.ToggleView)
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
                        Text(state.titleAr, color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        state.customer?.let { Text(it.nameAr, color = Fv.TextMid, fontSize = 11.sp) }
                    }
                    CartToggle(state.view, state.cart.size) { viewModel.onEvent(VoucherEvent.ToggleView) }
                }
            }

            // ── RETURN: reference to the original sale invoice ─────────────────
            if (state.type == VoucherType.RETURN) {
                ReturnReferenceBanner(
                    referenceNumber = state.referenceNumber,
                    onPick = { viewModel.onEvent(VoucherEvent.OpenSourcePicker) },
                )
            }

            // ── Views ─────────────────────────────────────────────────────────
            when (state.view) {
                VoucherView.PICKER -> {
                    ProductListPicker(
                        products = displayProducts,
                        allProducts = state.products,
                        productUnits = state.productUnits,
                        searchQuery = state.searchQuery,
                        selectedCategory = selectedCategory,
                        showStockBadge = state.showStockBadge,
                        onSearch = { viewModel.onEvent(VoucherEvent.SearchChanged(it)) },
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
                        onNotesChange = { viewModel.onEvent(VoucherEvent.NotesChanged(it)) },
                        onReasonSelect = { viewModel.onEvent(VoucherEvent.ReasonSelected(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    CartSummaryCard(
                        state = state,
                        onDiscountInputChange = { viewModel.onEvent(VoucherEvent.VoucherDiscountInputChanged(it)) },
                        onDiscountTypeToggle = { viewModel.onEvent(VoucherEvent.VoucherDiscountTypeToggled) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 6.dp),
                    )
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
                                    if (state.canSave) Modifier.background(Brush.linearGradient(saveGradient))
                                    else Modifier.background(Fv.SurfaceTop),
                                )
                                .clickable(enabled = state.canSave) {
                                    viewModel.onEvent(VoucherEvent.Save)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.saveLabelAr,
                                color = if (state.canSave) Color.White else Fv.TextMid,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Item bottom sheet ─────────────────────────────────────────────────────
    dialogProduct?.let { product ->
        val currentLine = state.cart.firstOrNull { it.productId == product.id }
        AddItemBottomSheet(
            product = product,
            currentLine = currentLine,
            dbUnits = state.productUnits[product.id] ?: emptyList(),
            enforceStock = state.showStockBadge,
            onConfirm = { qty, unit, unitPrice, unitConversionQty, discountPct ->
                viewModel.onEvent(
                    VoucherEvent.ConfirmItemDialog(product, qty, unit, unitPrice, unitConversionQty, discountPct),
                )
                dialogProduct = null
            },
            onDelete = if (currentLine != null) {
                { viewModel.onEvent(VoucherEvent.RemoveLine(currentLine.productId)); dialogProduct = null }
            } else null,
            onDismiss = { dialogProduct = null },
        )
    }

    // ── RETURN: source sale-invoice picker ─────────────────────────────────────
    if (state.showSourcePicker) {
        SourceInvoicePickerDialog(
            invoices = state.sourceInvoices,
            lookupQuery = state.sourceLookupQuery,
            isLookingUp = state.isLookingUp,
            onLookupChange = { viewModel.onEvent(VoucherEvent.SourceLookupChanged(it)) },
            onLookup = { viewModel.onEvent(VoucherEvent.LookupSource) },
            onSelect = { viewModel.onEvent(VoucherEvent.SelectSourceInvoice(it)) },
            onDismiss = { viewModel.onEvent(VoucherEvent.DismissSourcePicker) },
        )
    }

    // ── Save sheet: PaymentMethodDialog for SALE, AlertDialog for RETURN/ORDER
    if (state.showSaveSheet) {
        if (state.showPaymentDialog) {
            PaymentMethodDialog(
                current = state.paymentMethod,
                onSelect = { viewModel.onEvent(VoucherEvent.PaymentMethodSelected(it)) },
                onConfirm = { viewModel.onEvent(VoucherEvent.ConfirmSave) },
                onDismiss = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) },
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) },
                title = { Text(stringResource(Res.string.voucher_confirm_save_title), color = Fv.TextHigh) },
                text = { Text(state.confirmTextAr, color = Fv.TextHigh) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(VoucherEvent.ConfirmSave) }) {
                        Text(
                            stringResource(Res.string.confirm),
                            color = when (state.type) {
                                VoucherType.RETURN -> Fv.Red
                                VoucherType.ORDER -> Fv.Teal
                                else -> Fv.Green
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) }) {
                        Text(stringResource(Res.string.cancel), color = Fv.TextMid)
                    }
                },
                containerColor = Fv.Surface,
            )
        }
    }

    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(VoucherEvent.DismissError) },
            title = { Text(stringResource(Res.string.dialog_error_title), color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(VoucherEvent.DismissError) }) {
                    Text(stringResource(Res.string.dialog_ok), color = Fv.Blue)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

// ── Cart Toggle ───────────────────────────────────────────────────────────────

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
    showStockBadge: Boolean,
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
            placeholder = { Text(stringResource(Res.string.sale_search_product), color = Fv.TextMid) },
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
                CategoryPill(stringResource(Res.string.all), selectedCategory == null) { onSelectCategory(null) }
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
                    showStockBadge = showStockBadge,
                    onTap = { onTapProduct(product) },
                )
            }
            if (products.isEmpty()) {
                item {
                    Text(stringResource(Res.string.voucher_no_products), color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
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
    showStockBadge: Boolean,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductAvatar(
                seed = product.category,
                letter = product.nameAr.firstOrNull()?.toString() ?: "؟",
                size = 54.dp,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.nameAr,
                    color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(product.sku, color = Fv.TextMid, fontSize = 11.sp)
                    if (showStockBadge) StockBadge(product)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(unitPrice.formatJod(AppLanguage.AR), color = Fv.Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        outOfStock -> stringResource(Res.string.van_stock_out_of_stock)
        low -> stringResource(Res.string.voucher_stock_low_count, product.vanStock)
        else -> stringResource(Res.string.voucher_stock_available_count, product.vanStock)
    }
    Box(
        modifier = Modifier.background(chipColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
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
        Text(label, color = if (active) Color.White else Fv.TextMid, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun PickerSummaryBar(itemCount: Int, total: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2A3A)).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(total.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.voucher_item_count, itemCount), color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}

// ── Cart View ─────────────────────────────────────────────────────────────────

@Composable
private fun CartView(
    state: VoucherState,
    onTapLine: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onReasonSelect: (ReturnReason) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
    ) {
        items(state.cart, key = { it.productId }) { line ->
            CartItemCard(line = line, onTap = { onTapLine(line.productId) })
        }
        if (state.cart.isEmpty()) {
            item {
                Text(stringResource(Res.string.voucher_cart_empty_hint), color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
            }
        }
        if (state.showReasonRow) {
            item { ReasonRow(state.reason, onReasonSelect) }
        }

        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(Res.string.voucher_notes_optional), color = Fv.TextMid, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
        }
    }
}

// ── Reason Row (shown only for RETURN via state.showReasonRow) ────────────────

@Composable
private fun ReasonRow(selected: ReturnReason?, onSelect: (ReturnReason) -> Unit) {
    Column {
        Text("${stringResource(Res.string.return_reason)} *", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReturnReason.entries.forEach { reason ->
                val active = reason == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) Fv.Red else Fv.SurfaceTop, RoundedCornerShape(20.dp))
                        .clickable { onSelect(reason) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(reason.labelAr, color = if (active) Color.White else Fv.TextMid, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
    }
}

// ── Delivery Date Field (shown only for ORDER via state.showDeliveryDate) ─────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryDateField(dateMillis: Long?, onChange: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val label = dateMillis?.let { millis ->
        val dt = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
    } ?: stringResource(Res.string.voucher_delivery_date_placeholder)

    Column {
        Text(
            stringResource(Res.string.voucher_delivery_date_label),
            color = Fv.TextMid,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Fv.Surface)
                .border(1.dp, Fv.Border, RoundedCornerShape(10.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                label,
                color = if (dateMillis != null) Fv.TextHigh else Fv.TextMid,
                fontSize = 13.sp,
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(pickerState.selectedDateMillis)
                    showPicker = false
                }) { Text(stringResource(Res.string.confirm), color = Fv.Blue) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

// ── Cart Summary Card ─────────────────────────────────────────────────────────

@Composable
private fun CartSummaryCard(
    state: VoucherState,
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
                    SummaryDetailRow(stringResource(Res.string.voucher_detail_subtotal), state.subtotal.formatJod(AppLanguage.AR), Fv.TextMid)
                    if (state.lineDiscountTotal > 0)
                        SummaryDetailRow(stringResource(Res.string.voucher_line_discount_total), "- ${state.lineDiscountTotal.formatJod(AppLanguage.AR)}", Fv.Red)
                    if (state.showDiscountSection) {
                        Spacer(Modifier.height(8.dp))
                        VoucherDiscountSection(
                            type = state.voucherDiscountType,
                            input = state.voucherDiscountInput,
                            computedAmount = state.voucherDiscountAmount,
                            onInputChange = onDiscountInputChange,
                            onTypeToggle = onDiscountTypeToggle,
                        )
                    }
                    if (state.taxAmount > 0) {
                        Spacer(Modifier.height(6.dp))
                        SummaryDetailRow(state.taxLabelAr, state.taxAmount.formatJod(AppLanguage.AR), Fv.TextMid)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.voucher_final_total), color = Fv.TextMid, fontSize = 12.sp)
                    Text(state.total.formatJod(AppLanguage.AR), color = Fv.Blue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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

@Composable
private fun VoucherDiscountSection(
    type: DiscountType,
    input: String,
    computedAmount: Double,
    onInputChange: (String) -> Unit,
    onTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.background(Fv.SurfaceTop, RoundedCornerShape(20.dp)).padding(2.dp)) {
            listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to stringResource(Res.string.voucher_discount_type_value)).forEach { (t, label) ->
                val active = t == type
                Box(
                    modifier = Modifier
                        .clickable { if (!active) onTypeToggle() }
                        .background(if (active) Fv.Blue else Color.Transparent, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (active) Color.White else Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { v -> onInputChange(v.filter { it.isDigit() || it == '.' }.take(8)) },
            placeholder = { Text(if (type == DiscountType.PERCENT) stringResource(Res.string.voucher_discount_hint_percent) else stringResource(Res.string.voucher_discount_hint_value), color = Fv.TextMid, fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(if (type == DiscountType.PERCENT) "%" else "د.أ", color = Fv.TextMid, fontSize = 11.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = fvFieldColors(),
        )
        if (computedAmount > 0) {
            Text("- ${computedAmount.formatJod(AppLanguage.AR)}", color = Fv.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Add Item Bottom Sheet — exact same as SaleVoucherScreen ──────────────────

@Composable
private fun AddItemBottomSheet(
    product: Product,
    currentLine: CartLine?,
    dbUnits: List<ProductUnit>,
    enforceStock: Boolean,
    onConfirm: (qty: Double, unit: String, unitPrice: Double, unitConversionQty: Double, discountPct: Double) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val effectiveUnits: List<ProductUnit> = remember(product.id, dbUnits) {
        if (dbUnits.isNotEmpty()) dbUnits
        else (listOf(product.unit) + standardUnits)
            .filter { it.isNotBlank() }.distinct()
            .map { name -> ProductUnit(id = name, productId = product.id, name = name, price = product.salePrice, conversionQty = 1.0) }
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

    // Stock check (SALE only): the requested quantity is converted to base units via the
    // selected unit's pack size, then validated against what's available in the van.
    val availableBase = product.vanStock.toDouble()
    val requestedBase = qty * selectedUnit.conversionQty
    val maxQtyForUnit = if (selectedUnit.conversionQty > 0.0)
        floor(availableBase / selectedUnit.conversionQty).toInt() else 0
    val exceedsStock = enforceStock && requestedBase > availableBase
    val canIncrement = !enforceStock || qty < maxQtyForUnit
    val canConfirm = qty > 0 && !exceedsStock

    val blueGradient = Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C)))
    val greenGradient = Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56)))
    val heroGradient = Brush.linearGradient(listOf(Color(0xFFEEF4FF), Color(0xFFE6F1FB), Color(0xFFE1F5EE)))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column {
                    // Drag handle
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFDDE8F5), RoundedCornerShape(2.dp)))
                    }

                    // Hero
                    Box(modifier = Modifier.fillMaxWidth().background(heroGradient)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp).background(Color.White, RoundedCornerShape(18.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(blueGradient),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(product.nameAr.firstOrNull()?.toString() ?: "؟", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                product.nameAr,
                                color = Color(0xFF1A2A3A), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(blueGradient).padding(horizontal = 14.dp, vertical = 4.dp),
                            ) {
                                Text("${selectedUnit.price.formatJod(AppLanguage.AR)} / ${selectedUnit.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.88f))
                                .border(0.5.dp, Color(0xFFB5D4F4), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(product.sku, color = Color(0xFF185FA5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Body
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Qty stepper
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(Res.string.voucher_qty_required), color = Color(0xFF5A7399), fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(38.dp).background(Color.White, CircleShape).border(0.5.dp, Color(0xFFC8D8EC), CircleShape).clickable(enabled = qty > 1) { qty -= 1 },
                                    contentAlignment = Alignment.Center,
                                ) { Text("−", color = Color(0xFF185FA5), fontSize = 22.sp, fontWeight = FontWeight.Medium) }
                                Text(qty.toInt().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2A3A))
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (canIncrement) blueGradient else Brush.linearGradient(listOf(Color(0xFFC8D8EC), Color(0xFFC8D8EC))))
                                        .clickable(enabled = canIncrement) { qty += 1 },
                                    contentAlignment = Alignment.Center,
                                ) { Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                            }
                        }

                        // Unit dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(14.dp)).clickable { unitDropdownExpanded = true }.padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▾", color = Color(0xFF185FA5), fontSize = 18.sp)
                                Text(unitLabel(selectedUnit), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A2A3A))
                            }
                            DropdownMenu(expanded = unitDropdownExpanded, onDismissRequest = { unitDropdownExpanded = false }) {
                                effectiveUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unitLabel(unit), fontWeight = FontWeight.SemiBold) },
                                        onClick = { selectedUnit = unit; unitDropdownExpanded = false },
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDE8F5)))

                        // Stock check (SALE) — available van stock vs. requested base units
                        if (enforceStock) {
                            val warn = exceedsStock
                            val accent = if (warn) Color(0xFFE24B4A) else Color(0xFF1D9E75)
                            val bg = if (warn) Color(0xFFFFF0F0) else Color(0xFFEAF7F1)
                            val border = if (warn) Color(0xFFF7C1C1) else Color(0xFFBFE6D5)
                            val requestedLabel = if (requestedBase % 1.0 == 0.0)
                                requestedBase.toInt().toString() else requestedBase.toString()
                            Column(
                                modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(14.dp))
                                    .border(0.5.dp, border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(Res.string.voucher_stock_available_count, product.vanStock),
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent,
                                    )
                                    Text(
                                        stringResource(Res.string.voucher_stock_required_units, requestedLabel),
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5A7399),
                                    )
                                }
                                if (warn) {
                                    Text(
                                        stringResource(Res.string.voucher_stock_exceeds_warning),
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE24B4A),
                                    )
                                }
                            }
                        }

                        // Unit price display
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(selectedUnit.price.formatJod(AppLanguage.AR), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF185FA5))
                            Text("· ${stringResource(Res.string.voucher_unit_price)}", fontSize = 13.sp, color = Color(0xFF5A7399))
                        }

                        // Line discount
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(10.dp))) {
                                    listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to stringResource(Res.string.voucher_discount_type_value)).forEach { (t, label) ->
                                        val active = t == lineDiscountType
                                        Box(
                                            modifier = Modifier.clickable { if (!active) { lineDiscountType = t; discountText = "" } }.then(if (active) Modifier.background(blueGradient) else Modifier).padding(horizontal = 14.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(label, color = if (active) Color.White else Color(0xFF8A9AB0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                Text(stringResource(Res.string.voucher_line_discount), color = Color(0xFF8A9AB0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { v -> discountText = v.filter { it.isDigit() || it == '.' }.take(8) },
                                placeholder = { Text("0", color = Color(0xFFB0BEC5)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text(if (lineDiscountType == DiscountType.PERCENT) "%" else "د.أ", color = Color(0xFF185FA5), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fvFieldColors(),
                            )
                        }

                        // Total card
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(blueGradient).padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(lineTotal.formatJod(AppLanguage.AR), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(stringResource(Res.string.voucher_line_total), fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }

                    // Actions
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (onDelete != null) {
                            Box(
                                modifier = Modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF0F0)).border(0.5.dp, Color(0xFFF7C1C1), RoundedCornerShape(16.dp)).clickable { onDelete() }.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(stringResource(Res.string.delete), color = Color(0xFFE24B4A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(16.dp)).clickable { onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(Res.string.cancel), color = Color(0xFF5A7399), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            modifier = Modifier.weight(if (onDelete != null) 1.8f else 2f).height(52.dp).clip(RoundedCornerShape(16.dp))
                                .then(if (canConfirm) Modifier.background(greenGradient) else Modifier.background(Fv.SurfaceTop))
                                .clickable(enabled = canConfirm) {
                                    onConfirm(qty, selectedUnit.name, selectedUnit.price, selectedUnit.conversionQty, discountPct)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_cart),
                                    contentDescription = null,
                                    tint = if (canConfirm) Color.White else Fv.TextMid,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    if (currentLine == null) stringResource(Res.string.voucher_add_to_cart) else stringResource(Res.string.voucher_update_line),
                                    color = if (canConfirm) Color.White else Fv.TextMid,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
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

// ── Payment Method Dialog (SALE only) ────────────────────────────────────────

@Composable
private fun PaymentMethodDialog(
    current: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sale_payment_method), color = Fv.TextHigh) },
        text = {
            Column {
                listOf(PaymentMethod.CASH to stringResource(Res.string.payment_method_cash), PaymentMethod.CREDIT to stringResource(Res.string.payment_method_credit)).forEach { (method, label) ->
                    val active = method == current
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(method) }
                            .background(if (active) Fv.Blue else Fv.SurfaceHigh, RoundedCornerShape(10.dp)).padding(12.dp),
                    ) {
                        Text(label, color = if (active) Color.White else Fv.TextMid, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.confirm), color = Fv.Green) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) } },
        containerColor = Fv.Surface,
    )
}

// ── RETURN: reference to original sale invoice ────────────────────────────────

@Composable
private fun ReturnReferenceBanner(referenceNumber: String?, onPick: () -> Unit) {
    val hasRef = referenceNumber != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hasRef) Color(0xFFEFF6FF) else Color(0xFFFFF4E5))
            .clickable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasRef) {
            Text(
                stringResource(Res.string.return_reference_label, referenceNumber!!),
                color = Fv.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(Res.string.return_change), color = Fv.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(stringResource(Res.string.return_source_pick), color = Fv.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("+", color = Fv.Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SourceInvoicePickerDialog(
    invoices: List<InvoiceEntity>,
    lookupQuery: String,
    isLookingUp: Boolean,
    onLookupChange: (String) -> Unit,
    onLookup: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.return_source_title), color = Fv.TextHigh) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Manual lookup: find a sale by number on the server when it isn't local.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = lookupQuery,
                        onValueChange = onLookupChange,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ابحث برقم فاتورة البيع", color = Fv.TextMid, fontSize = 13.sp) },
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = onLookup,
                        enabled = !isLookingUp && lookupQuery.isNotBlank(),
                    ) {
                        Text(if (isLookingUp) "..." else "بحث", color = Fv.Blue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (invoices.isEmpty()) {
                    Text(stringResource(Res.string.return_source_empty), color = Fv.TextMid)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(invoices) { inv ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Fv.SurfaceTop)
                                .clickable { onSelect(inv.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${inv.number}", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(inv.total.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(formatInvoiceDate(inv.createdAt), color = Fv.TextMid, fontSize = 11.sp)
                        }
                    }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) } },
        containerColor = Fv.Surface,
    )
}

private fun formatInvoiceDate(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
}
