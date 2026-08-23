package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jehadalomour.flowvan.core.designsystem.components.CartItemCard
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.ProductPickerColumn
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.network.dto.StockRequestDto
import org.koin.compose.viewmodel.koinViewModel

/**
 * Request stock for the van — the voucher cart flow, with the money taken out.
 *
 * Picker → tap an item → choose unit and quantity → cart → send. Identical to
 * making a sale, deliberately: a rep does this between calls, and a second
 * item-entry idiom is a second thing to get wrong in a hurry.
 *
 * What is different is the total. A stock request totals PIECES, not dinars, so
 * the sale's TotalsStrip is not reused — it formats currency, and a quantity
 * rendered as money would read as a value the request does not have.
 */
@Composable
fun StockRequestScreen(
    onBack: () -> Unit,
    viewModel: StockRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    /** The item whose sheet is open, plus which of its unit lines was tapped. */
    var sheetProduct by remember { mutableStateOf<Product?>(null) }
    var sheetUnitId by remember { mutableStateOf<String?>(null) }

    // In the cart, back returns to the picker rather than leaving the screen —
    // otherwise one stray back gesture discards a cart the rep just built.
    AppBackHandler(enabled = state.view == StockRequestView.CART) {
        viewModel.onEvent(StockRequestEvent.ToggleView)
    }

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Fv.Surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "رجوع",
                    color = Fv.Blue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (state.view == StockRequestView.CART) {
                            viewModel.onEvent(StockRequestEvent.ToggleView)
                        } else {
                            onBack()
                        }
                    },
                )
                Spacer(Modifier.width(14.dp))
                Text("طلب بضاعة", color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                // The cart badge doubles as the way into the cart, so the count and
                // the way to act on it are never in two different places.
                Box(
                    modifier = Modifier
                        .background(
                            if (state.cart.isEmpty()) Fv.SurfaceTop else Fv.Blue,
                            RoundedCornerShape(9.dp),
                        )
                        .clickable(enabled = state.cart.isNotEmpty()) {
                            viewModel.onEvent(StockRequestEvent.ToggleView)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (state.view == StockRequestView.CART) {
                            "الأصناف"
                        } else {
                            "السلة (${state.lineCount})"
                        },
                        color = if (state.cart.isEmpty()) Fv.TextLow else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        state.errorAr?.let { Banner(it, Fv.Red) { viewModel.onEvent(StockRequestEvent.DismissError) } }
        state.noticeAr?.let { Banner(it, Fv.Green) { viewModel.onEvent(StockRequestEvent.DismissError) } }

        when (state.view) {
            StockRequestView.PICKER -> ProductPickerColumn(
                products = state.visibleProducts,
                searchQuery = state.searchQuery,
                onSearch = { viewModel.onEvent(StockRequestEvent.SearchChanged(it)) },
                onAdd = { sheetProduct = it; sheetUnitId = null },
                // Van stock is context here, not a limit: being low is the REASON
                // to ask, so a badge informs and nothing is capped by it.
                showStockBadge = true,
                showPrice = false,
                cartQtyMap = state.cartQtyByProduct,
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
            )

            StockRequestView.CART -> CartView(
                state = state,
                onTapLine = { productId, unitId ->
                    sheetProduct = state.products.firstOrNull { it.id == productId }
                    sheetUnitId = unitId
                },
                onNote = { viewModel.onEvent(StockRequestEvent.NoteChanged(it)) },
                onSubmit = { viewModel.onEvent(StockRequestEvent.Submit) },
                onAddMore = { viewModel.onEvent(StockRequestEvent.ToggleView) },
                onRefresh = { viewModel.onEvent(StockRequestEvent.Refresh) },
                onCancel = { viewModel.onEvent(StockRequestEvent.Cancel(it)) },
                onReceive = { viewModel.onEvent(StockRequestEvent.Receive(it)) },
            )
        }
    }

    sheetProduct?.let { product ->
        StockItemSheet(
            product = product,
            cartLines = state.cart.filter { it.productId == product.id },
            initialUnitId = sheetUnitId,
            dbUnits = state.unitsFor(product.id),
            availableFor = { unit -> state.availableBase(product.sku, unit) },
            onConfirm = { qty, unit ->
                viewModel.onEvent(StockRequestEvent.ConfirmItem(product, qty, unit))
                sheetProduct = null
                sheetUnitId = null
            },
            onDelete = { unitId ->
                viewModel.onEvent(StockRequestEvent.RemoveLine(product.id, unitId))
                sheetProduct = null
                sheetUnitId = null
            },
            onDismiss = { sheetProduct = null; sheetUnitId = null },
        )
    }
}

// ── Cart ─────────────────────────────────────────────────────────────────────

@Composable
private fun CartView(
    state: StockRequestState,
    onTapLine: (productId: String, unitId: String) -> Unit,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddMore: () -> Unit,
    onRefresh: () -> Unit,
    onCancel: (String) -> Unit,
    onReceive: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        if (state.cart.isEmpty()) {
            item {
                PlainCard {
                    Text(
                        "السلة فارغة. ارجع لقائمة الأصناف واختر ما تحتاجه.",
                        color = Fv.TextMid,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        // Composite key: two units of one item under a single productId key is a
        // LazyColumn crash ("Key was already used").
        items(state.cart, key = { it.key }) { line ->
            CartItemCard(
                line = line,
                onTap = { onTapLine(line.productId, line.unitId) },
                showPrice = false,
                // Only when the unit actually converts — "= 4 حبة" beside a 4
                // entered in base pieces is noise.
                baseQtyLabel = if (line.unitConversionQty > 1) {
                    "= ${line.stockQty.toInt()} حبة"
                } else {
                    null
                },
            )
        }

        if (state.cart.isNotEmpty()) {
            item {
                // The stock request's answer to TotalsStrip: pieces, not dinars.
                PlainCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("إجمالي المطلوب", color = Fv.TextMid, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${state.totalBaseQty.toInt()} حبة",
                            color = Fv.TextHigh,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            item {
                PlainCard {
                    Column(Modifier.padding(12.dp)) {
                        Text("ملاحظة للإدارة", color = Fv.TextMid, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = onNote,
                            placeholder = { Text("مثال: شارف على النفاد قبل الخميس", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("+ أصناف أخرى", Fv.Blue) { onAddMore() }
                    Spacer(Modifier.weight(1f))
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (state.canSubmit) Fv.Blue else Fv.SurfaceTop,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = state.canSubmit) { onSubmit() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "إرسال الطلب",
                            color = if (state.canSubmit) Color.White else Fv.TextLow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("طلباتي", color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (state.isLoadingMine) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Fv.TextLow,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Pill("تحديث", Fv.TextMid) { onRefresh() }
                }
            }
        }

        if (state.mine.isEmpty() && !state.isLoadingMine) {
            item {
                PlainCard {
                    Text(
                        "لا توجد طلبات سابقة.",
                        color = Fv.TextMid,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        items(state.mine, key = { it.id }) { req ->
            RequestCard(
                req = req,
                busy = req.id in state.busyIds,
                onCancel = { onCancel(req.id) },
                onReceive = { onReceive(req.id) },
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Item sheet: unit + quantity ──────────────────────────────────────────────

/** "كرتونة (١٢ حبة)" — the pack size is what tells two units apart at a glance. */
private fun unitLabel(unit: ProductUnit): String =
    if (unit.conversionQty > 1) "${unit.name} (${unit.conversionQty.toInt()} حبة)" else unit.name

/**
 * Choose a unit and a quantity for one item.
 *
 * Modelled on the sale's AddItemBottomSheet, minus price, discount and the stock
 * cap. The cap is the important omission: a rep asks for stock precisely BECAUSE
 * the van is low, so capping the request at what is already on board would block
 * the only case this screen exists for.
 */
@Composable
private fun StockItemSheet(
    product: Product,
    /** Every cart line of this item — one per unit. The sheet edits the selected one. */
    cartLines: List<CartLine>,
    initialUnitId: String?,
    dbUnits: List<ProductUnit>,
    /** Main-depot pieces available for a unit's pool. Caps the request. */
    availableFor: (ProductUnit) -> Double,
    onConfirm: (qty: Double, unit: ProductUnit) -> Unit,
    onDelete: (unitId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // The item's own units, as synced. An item with none falls back to a single
    // base unit whose id stays BLANK on purpose: it is not a real item_units row,
    // and posting a made-up id would be rejected by the server.
    val effectiveUnits: List<ProductUnit> = remember(product.id, dbUnits) {
        dbUnits.ifEmpty {
            listOf(
                ProductUnit(
                    id = "",
                    productId = product.id,
                    name = product.unit,
                    price = 0.0,
                    conversionQty = 1.0,
                ),
            )
        }
    }

    val initialUnit: ProductUnit = remember(product.id, initialUnitId, effectiveUnits) {
        // Resolve BY ID. Two variants of one item can share a display name, so a
        // name match would open the wrong line.
        effectiveUnits.firstOrNull { initialUnitId != null && it.id == initialUnitId }
            ?: effectiveUnits.firstOrNull()
            ?: ProductUnit(id = "", productId = product.id, name = product.unit, price = 0.0, conversionQty = 1.0)
    }
    var selectedUnit by remember(product.id, initialUnitId) { mutableStateOf(initialUnit) }

    // Switching unit in the dropdown moves to THAT unit's line, or to a fresh add
    // when the rep has not entered that unit yet.
    val currentLine = cartLines.firstOrNull { it.unitId == selectedUnit.id }
    var qty by remember(product.id, selectedUnit.id) { mutableStateOf(currentLine?.qty ?: 1.0) }
    var qtyText by remember(product.id, selectedUnit.id) { mutableStateOf(qty.toInt().toString()) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val onVan =
        if (selectedUnit.isStockUnit) selectedUnit.vanStock else product.vanStock
    val requestedBase = qty * selectedUnit.conversionQty
    val availableBase = availableFor(selectedUnit)
    val overMain = requestedBase > availableBase + 1e-6

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.width(40.dp).height(4.dp)
                                .background(Color(0xFFDDE8F5), RoundedCornerShape(2.dp)),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            product.nameAr,
                            color = Fv.TextHigh,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${product.sku} · على المركبة $onVan · بالمستودع ${trimQtyLabel(availableBase)}",
                            color = if (overMain) Fv.Red else Fv.TextLow,
                            fontSize = 11.sp,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Qty stepper
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("الكمية", color = Color(0xFF5A7399), fontSize = 14.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp)
                                        .background(Color.White, CircleShape)
                                        .border(0.5.dp, Color(0xFFC8D8EC), CircleShape)
                                        .clickable(enabled = qty > 1) {
                                            qty -= 1; qtyText = qty.toInt().toString()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("−", color = Fv.Blue, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                }
                                BasicTextField(
                                    value = qtyText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(6)
                                        qtyText = digits
                                        qty = digits.toIntOrNull()?.toDouble() ?: 0.0
                                    },
                                    modifier = Modifier.width(88.dp),
                                    textStyle = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Fv.Blue,
                                        textAlign = TextAlign.Center,
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(Fv.Blue),
                                    decorationBox = { inner ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White, RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF9EC3EA), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { inner() }
                                    },
                                )
                                Box(
                                    modifier = Modifier.size(38.dp)
                                        .background(Fv.Blue, CircleShape)
                                        .clickable { qty += 1; qtyText = qty.toInt().toString() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Unit dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(14.dp))
                                    .clickable { unitDropdownExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▾", color = Fv.Blue, fontSize = 18.sp)
                                Text(
                                    unitLabel(selectedUnit),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Fv.TextHigh,
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

                        // The conversion, shown only when it changes the number —
                        // "= 4 حبة" beside a 4 in base units is noise.
                        if (selectedUnit.conversionQty > 1 && qty > 0) {
                            Text(
                                "= ${requestedBase.toInt()} حبة",
                                color = Fv.TextMid,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (currentLine != null) {
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(Fv.Red.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .clickable { onDelete(selectedUnit.id) }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("حذف", color = Fv.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(2f)
                                .background(
                                    if (qty > 0 && !overMain) Fv.Blue else Fv.SurfaceTop,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable(enabled = qty > 0 && !overMain) { onConfirm(qty, selectedUnit) }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (overMain) "يتجاوز المتوفر" else if (currentLine != null) "تعديل" else "إضافة للسلة",
                                color = if (qty > 0 && !overMain) Color.White else Fv.TextLow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Small pieces ─────────────────────────────────────────────────────────────

@Composable
private fun Banner(text: String, tone: Color, onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = tone.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = tone, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                "إغلاق",
                color = tone,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onDismiss() },
            )
        }
    }
}

@Composable
private fun PlainCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Fv.Surface,
        shape = RoundedCornerShape(12.dp),
    ) { content() }
}

@Composable
private fun Pill(text: String, tone: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(tone.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = tone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Fv.SurfaceHigh,
    unfocusedContainerColor = Fv.SurfaceHigh,
    focusedIndicatorColor = Fv.Blue,
    unfocusedIndicatorColor = Fv.Border,
    focusedTextColor = Fv.TextHigh,
    unfocusedTextColor = Fv.TextHigh,
)

/** pending | approved | rejected | cancelled | received → label and colour. */
private fun statusLabel(status: String): Pair<String, Color> = when (status) {
    "pending" -> "قيد الانتظار" to Fv.Amber
    "approved" -> "بانتظار الاستلام" to Fv.Green
    "received" -> "تم الاستلام" to Fv.Blue
    "rejected" -> "مرفوض" to Fv.Red
    "cancelled" -> "ملغى" to Fv.TextLow
    else -> status to Fv.TextMid
}

@Composable
private fun RequestCard(
    req: StockRequestDto,
    busy: Boolean,
    onCancel: () -> Unit,
    onReceive: () -> Unit,
) {
    val (label, tone) = statusLabel(req.status)
    PlainCard {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    req.requestNumber,
                    color = Fv.TextHigh,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(tone.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(label, color = tone, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(6.dp))
            req.items.forEach { l ->
                val approved = l.approvedBaseQty?.toDoubleOrNull()
                val asked = l.baseQty.toDoubleOrNull() ?: 0.0
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        l.itemName,
                        color = Fv.TextMid,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    // Two numbers only when they differ — an unchanged grant shown
                    // twice reads as a discrepancy at a glance.
                    Text(
                        when {
                            approved == null -> "${asked.toInt()}"
                            approved < asked -> "${approved.toInt()} من ${asked.toInt()}"
                            else -> "${approved.toInt()}"
                        },
                        color = if (approved != null && approved < asked) Fv.Amber else Fv.TextHigh,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            req.decisionNote?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Fv.TextMid, fontSize = 12.sp)
            }

            req.transferVoucherNumber?.let {
                Spacer(Modifier.height(4.dp))
                Text("سند التحويل: $it", color = Fv.TextLow, fontSize = 11.sp)
            }

            if (req.status == "pending" || req.status == "approved") {
                Spacer(Modifier.height(10.dp))
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Fv.TextLow,
                        strokeWidth = 2.dp,
                    )
                } else if (req.status == "approved") {
                    // The action that actually moves stock. Worded as the physical
                    // act, not the system one: the rep is looking at boxes.
                    var confirming by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Fv.Green, RoundedCornerShape(9.dp))
                            .clickable { confirming = true }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "استلمت البضاعة",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (confirming) {
                        // Receiving MOVES stock (raises a transfer). Make the rep
                        // confirm against the actual items+quantities first, so a
                        // stray tap cannot silently change the van's inventory.
                        val lines = req.items.map { l ->
                            val q = (l.approvedBaseQty?.toDoubleOrNull() ?: l.baseQty.toDoubleOrNull() ?: 0.0).toInt()
                            "${l.itemName} — $q حبة"
                        }
                        AlertDialog(
                            onDismissRequest = { confirming = false },
                            title = { Text("تأكيد استلام البضاعة") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("سيتم تحويل هذه الأصناف إلى مخزون مركبتك:", fontSize = 12.sp, color = Fv.TextMid)
                                    lines.forEach { Text("• $it", fontSize = 12.sp, color = Fv.TextHigh) }
                                }
                            },
                            confirmButton = {
                                Text(
                                    "تأكيد الاستلام",
                                    color = Fv.Green,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { confirming = false; onReceive() }.padding(8.dp),
                                )
                            },
                            dismissButton = {
                                Text(
                                    "إلغاء",
                                    color = Fv.TextMid,
                                    modifier = Modifier.clickable { confirming = false }.padding(8.dp),
                                )
                            },
                        )
                    }
                } else {
                    Pill("إلغاء الطلب", Fv.Red) { onCancel() }
                }
            }
        }
    }
}

/** Whole numbers without a decimal tail — for the availability label. */
private fun trimQtyLabel(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
