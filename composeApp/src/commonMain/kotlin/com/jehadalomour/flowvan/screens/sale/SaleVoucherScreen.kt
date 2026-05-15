package com.jehadalomour.flowvan.screens.sale

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.CartLineRow
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.ProductAvatar
import com.jehadalomour.flowvan.screens.components.ProductPickerColumn
import com.jehadalomour.flowvan.screens.components.TotalsStrip
import com.jehadalomour.flowvan.screens.components.fvFieldColors
import com.jehadalomour.flowvan.screens.components.standardUnits
import com.jehadalomour.flowvan.shared.domain.model.ProductUnit
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.presentation.feature.sale.DiscountType
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherEvent
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherState
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
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

    LaunchedEffect(state.savedNumber) {
        if (state.savedNumber != null) onBack()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed top bar — never scrolls
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("فاتورة بيع", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    state.customer?.let { Text(it.nameAr, color = Fv.TextMid, fontSize = 11.sp) }
                }
                CartToggle(state.view, state.cart.size) { viewModel.onEvent(SaleVoucherEvent.ToggleView) }
            }

            when (state.view) {
                VoucherView.PICKER -> {
                    ProductPickerColumn(
                        products = state.visibleProducts,
                        searchQuery = state.searchQuery,
                        onSearch = { viewModel.onEvent(SaleVoucherEvent.SearchChanged(it)) },
                        onAdd = { product -> dialogProduct = product },
                        showStockBadge = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        cartQtyMap = state.cart.associate { it.productId to it.qty },
                        onStep = { product, delta -> viewModel.onEvent(SaleVoucherEvent.StepItem(product, delta)) },
                    )
                }
                VoucherView.CART -> CartView(
                    state = state,
                    onChangeQty = { id, q -> viewModel.onEvent(SaleVoucherEvent.ChangeQty(id, q)) },
                    onRemove = { viewModel.onEvent(SaleVoucherEvent.RemoveLine(it)) },
                    onTapLine = { productId ->
                        dialogProduct = state.products.firstOrNull { it.id == productId }
                    },
                    onNotesChange = { viewModel.onEvent(SaleVoucherEvent.NotesChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
            }

            VoucherDiscountSection(
                type = state.voucherDiscountType,
                input = state.voucherDiscountInput,
                computedAmount = state.voucherDiscountAmount,
                onInputChange = { viewModel.onEvent(SaleVoucherEvent.VoucherDiscountInputChanged(it)) },
                onTypeToggle = { viewModel.onEvent(SaleVoucherEvent.VoucherDiscountTypeToggled) },
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            )
            TotalsStrip(
                subtotal = state.subtotal,
                lineDiscount = state.lineDiscountTotal.takeIf { it > 0 },
                voucherDiscount = state.voucherDiscountAmount.takeIf { it > 0 },
                tax = state.taxAmount,
                total = state.total,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Button(
                onClick = { viewModel.onEvent(SaleVoucherEvent.OpenSaveSheet) },
                enabled = state.cart.isNotEmpty() && !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fv.Green,
                    contentColor = Fv.BgDeepest,
                    disabledContainerColor = Fv.SurfaceTop,
                    disabledContentColor = Fv.TextMid,
                ),
            ) { Text("حفظ الفاتورة", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
        }
    }

    dialogProduct?.let { product ->
        val currentLine = state.cart.firstOrNull { it.productId == product.id }
        AddItemDialog(
            product = product,
            currentLine = currentLine,
            dbUnits = state.productUnits[product.id] ?: emptyList(),
            onConfirm = { qty, unit, unitPrice, unitConversionQty, discountPct ->
                viewModel.onEvent(SaleVoucherEvent.ConfirmItemDialog(product, qty, unit, unitPrice, unitConversionQty, discountPct))
                dialogProduct = null
            },
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

@Composable
private fun CartToggle(view: VoucherView, count: Int, onClick: () -> Unit) {
    val isPickerView = view == VoucherView.PICKER
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isPickerView && count > 0) Fv.Blue.copy(alpha = 0.14f) else Fv.SurfaceTop,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(if (isPickerView) Res.drawable.ic_cart else Res.drawable.ic_inventory),
                contentDescription = null,
                tint = if (isPickerView && count > 0) Fv.Blue else Fv.TextMid,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (isPickerView) "السلة" else "المنتجات",
                color = if (isPickerView && count > 0) Fv.Blue else Fv.TextMid,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (isPickerView && count > 0) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier.size(18.dp).background(Fv.Blue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(count.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CartView(
    state: SaleVoucherState,
    onChangeQty: (String, Double) -> Unit,
    onRemove: (String) -> Unit,
    onTapLine: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
    ) {
        items(state.cart, key = { it.productId }) { line ->
            CartLineRow(
                line = line,
                onChangeQty = { onChangeQty(line.productId, it) },
                onRemove = { onRemove(line.productId) },
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
        // Toggle: [%] [قيمة]
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
                    Text(label, color = if (active) Color.White else Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { v -> onInputChange(v.filter { it.isDigit() || it == '.' }.take(8)) },
            placeholder = { Text(if (type == DiscountType.PERCENT) "خصم الفاتورة %" else "خصم الفاتورة د.أ", color = Fv.TextLow, fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(if (type == DiscountType.PERCENT) "%" else "د.أ", color = Fv.TextMid, fontSize = 11.sp) },
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

@Composable
private fun AddItemDialog(
    product: Product,
    currentLine: CartLine?,
    dbUnits: List<ProductUnit>,
    onConfirm: (qty: Double, unit: String, unitPrice: Double, unitConversionQty: Double, discountPct: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val effectiveUnits: List<ProductUnit> = remember(product.id, dbUnits) {
        if (dbUnits.isNotEmpty()) {
            dbUnits
        } else {
            (listOf(product.unit) + standardUnits)
                .filter { it.isNotBlank() }
                .distinct()
                .map { name -> ProductUnit(id = name, productId = product.id, name = name, price = product.salePrice, conversionQty = 1.0) }
        }
    }

    val initialUnit: ProductUnit = remember(product.id, currentLine) {
        val cartUnitName = currentLine?.unit?.takeIf { it.isNotBlank() }
        effectiveUnits.firstOrNull { it.name == cartUnitName }
            ?: effectiveUnits.firstOrNull()
            ?: ProductUnit(id = product.unit, productId = product.id, name = product.unit, price = product.salePrice, conversionQty = 1.0)
    }

    val initialQty = currentLine?.qty ?: 1.0
    val initialDiscountPct = currentLine?.discountPct ?: 0.0

    var qty by remember(product.id) { mutableStateOf(initialQty) }
    var selectedUnit by remember(product.id) { mutableStateOf(initialUnit) }
    var lineDiscountType by remember(product.id) { mutableStateOf(DiscountType.PERCENT) }
    var discountText by remember(product.id) {
        mutableStateOf(if (initialDiscountPct > 0) (initialDiscountPct * 100).toInt().toString() else "")
    }

    val gross = selectedUnit.price * qty
    val discountPct = when (lineDiscountType) {
        DiscountType.PERCENT -> discountText.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0) ?: 0.0
        DiscountType.VALUE   -> if (gross > 0) (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, gross) / gross else 0.0
    }
    val lineTotal = gross * (1.0 - discountPct)
    val hasDbUnits = dbUnits.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fv.Surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductAvatar(
                    seed = product.category,
                    letter = product.nameAr.firstOrNull()?.toString() ?: "؟",
                    size = 38.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(product.nameAr, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${product.sku} · ${selectedUnit.price.formatJod(AppLanguage.AR)}",
                        color = Fv.TextMid,
                        fontSize = 10.sp,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Qty stepper
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الكمية", color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                    DialogQtyStepper(qty = qty, onChange = { qty = it })
                }

                // Unit chips — show price per unit when DB units are loaded
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الوحدة", color = Fv.TextMid, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        effectiveUnits.forEach { unit ->
                            val active = unit.name == selectedUnit.name
                            Box(
                                modifier = Modifier
                                    .clickable { selectedUnit = unit }
                                    .background(
                                        if (active) Fv.Blue else Fv.SurfaceHigh,
                                        RoundedCornerShape(20.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        unit.name,
                                        color = if (active) Color.White else Fv.TextMid,
                                        fontSize = 12.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    if (hasDbUnits) {
                                        Text(
                                            unit.price.formatJod(AppLanguage.AR),
                                            color = if (active) Color.White.copy(alpha = 0.8f) else Fv.TextLow,
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected unit price — updates instantly when a chip is tapped
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Fv.SurfaceTop, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("سعر الوحدة", color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        selectedUnit.price.formatJod(AppLanguage.AR),
                        color = Fv.Blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Per-line discount with mode toggle
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("خصم السطر", color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        // % / قيمة toggle
                        Row(
                            modifier = Modifier
                                .background(Fv.SurfaceTop, RoundedCornerShape(20.dp))
                                .padding(2.dp),
                        ) {
                            listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to "قيمة").forEach { (t, label) ->
                                val active = t == lineDiscountType
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            if (!active) {
                                                lineDiscountType = t
                                                discountText = ""
                                            }
                                        }
                                        .background(if (active) Fv.Blue else Color.Transparent, RoundedCornerShape(18.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(label, color = if (active) Color.White else Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { v ->
                            discountText = v.filter { it.isDigit() || it == '.' }.take(8)
                        },
                        placeholder = { Text(if (lineDiscountType == DiscountType.PERCENT) "0" else "0.000", color = Fv.TextLow, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text(if (lineDiscountType == DiscountType.PERCENT) "%" else "د.أ", color = Fv.TextMid) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = fvFieldColors(),
                    )
                }

                // Live line total
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Fv.SurfaceHigh),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("إجمالي السطر", color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(
                            lineTotal.formatJod(AppLanguage.AR),
                            color = Fv.Green,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(qty, selectedUnit.name, selectedUnit.price, selectedUnit.conversionQty, discountPct) },
                enabled = qty > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Fv.Blue, contentColor = Color.White),
            ) {
                Text(if (currentLine == null) "إضافة للسلة" else "تحديث", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Fv.TextMid) }
        },
    )
}

@Composable
private fun DialogQtyStepper(qty: Double, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (qty > 1) Fv.SurfaceTop else Fv.Border, CircleShape)
                .clickable(enabled = qty > 1) { onChange(qty - 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("−", color = if (qty > 1) Fv.TextHigh else Fv.TextMid, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            qty.toInt().toString(),
            color = Fv.TextHigh,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Fv.Blue, CircleShape)
                .clickable { onChange(qty + 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

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
                    PaymentMethod.CHEQUE to "شيك",
                    PaymentMethod.TRANSFER to "تحويل",
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
                            color = if (active) Fv.TextHigh else Fv.TextMid,
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
