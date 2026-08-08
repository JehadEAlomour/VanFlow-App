package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.network.dto.StockRequestDto
import org.koin.compose.viewmodel.koinViewModel

/**
 * Request stock for the van.
 *
 * One screen, two halves: the request being built, and what the office has done
 * with the previous ones. They stay together because the rep's real question is
 * "am I getting the goods?", and a request that vanishes on send answers it with
 * a phone call to the office.
 */
@Composable
fun StockRequestScreen(
    onBack: () -> Unit,
    viewModel: StockRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                    modifier = Modifier.clickable { onBack() },
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "طلب بضاعة",
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        state.errorAr?.let { Banner(it, Fv.Red) { viewModel.onEvent(StockRequestEvent.DismissError) } }
        state.noticeAr?.let { Banner(it, Fv.Green) { viewModel.onEvent(StockRequestEvent.DismissError) } }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "الأصناف المطلوبة",
                        color = Fv.TextHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Pill("+ إضافة صنف", Fv.Blue) {
                        viewModel.onEvent(StockRequestEvent.OpenPicker)
                    }
                }
            }

            if (state.lines.isEmpty()) {
                item {
                    Card {
                        Text(
                            "لم تُضف أصناف بعد. اضغط «إضافة صنف» لاختيار ما تحتاجه.",
                            color = Fv.TextMid,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            itemsIndexed(state.lines) { index, line ->
                LineCard(
                    line = line,
                    hasUnits = state.unitsFor(line.product).isNotEmpty(),
                    onQty = { viewModel.onEvent(StockRequestEvent.QuantityChanged(index, it)) },
                    onUnit = { viewModel.onEvent(StockRequestEvent.OpenUnitPicker(index)) },
                    onRemove = { viewModel.onEvent(StockRequestEvent.RemoveLine(index)) },
                )
            }

            if (state.lines.isNotEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("ملاحظة للإدارة", color = Fv.TextMid, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state.note,
                                onValueChange = {
                                    viewModel.onEvent(StockRequestEvent.NoteChanged(it))
                                },
                                placeholder = {
                                    Text("مثال: شارف على النفاد قبل الخميس", fontSize = 12.sp)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                            )
                        }
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
                            .clickable(enabled = state.canSubmit) {
                                viewModel.onEvent(StockRequestEvent.Submit)
                            }
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
                    Text(
                        "طلباتي",
                        color = Fv.TextHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.isLoadingMine) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Fv.TextLow,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Pill("تحديث", Fv.TextMid) {
                            viewModel.onEvent(StockRequestEvent.Refresh)
                        }
                    }
                }
            }

            if (state.mine.isEmpty() && !state.isLoadingMine) {
                item {
                    Card {
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
                    onCancel = { viewModel.onEvent(StockRequestEvent.Cancel(req.id)) },
                    onReceive = { viewModel.onEvent(StockRequestEvent.Receive(req.id)) },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (state.isPickerOpen) {
        ItemPicker(
            products = state.products,
            query = state.searchQuery,
            onQuery = { viewModel.onEvent(StockRequestEvent.SearchChanged(it)) },
            onPick = { viewModel.onEvent(StockRequestEvent.AddProduct(it)) },
            onClose = { viewModel.onEvent(StockRequestEvent.ClosePicker) },
        )
    }

    state.unitPickerFor?.let { index ->
        val line = state.lines.getOrNull(index)
        if (line != null) {
            UnitPicker(
                units = state.unitsFor(line.product),
                selected = line.unit,
                onPick = { viewModel.onEvent(StockRequestEvent.UnitChanged(index, it)) },
                onClose = { viewModel.onEvent(StockRequestEvent.CloseUnitPicker) },
            )
        }
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

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
private fun Card(content: @Composable () -> Unit) {
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

@Composable
private fun LineCard(
    line: StockRequestLine,
    hasUnits: Boolean,
    onQty: (String) -> Unit,
    onUnit: () -> Unit,
    onRemove: () -> Unit,
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.product.nameAr,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${line.product.sku} · على المركبة ${line.onVan}",
                        color = Fv.TextLow,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    "حذف",
                    color = Fv.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onRemove() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = line.quantity,
                    onValueChange = onQty,
                    placeholder = { Text("0", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp),
                    colors = fieldColors(),
                )
                Spacer(Modifier.width(10.dp))
                if (hasUnits) {
                    Pill(line.unit?.name ?: "الوحدة", Fv.Blue) { onUnit() }
                }
                Spacer(Modifier.weight(1f))
                // The conversion is shown only when it changes the number. For a
                // base unit it would read "= 4" beside "4", which is noise.
                if (line.factor > 1 && line.qtyOrZero > 0) {
                    Text(
                        "= ${line.baseQty.toInt()} حبة",
                        color = Fv.TextMid,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

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
    Card {
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
                    // Only show two numbers when they differ — an unchanged grant
                    // repeated twice reads as a discrepancy at a glance.
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
                    // The action that actually moves stock. Worded as the
                    // physical act, not the system one: the rep is looking at
                    // boxes, not at a state machine.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Fv.Green, RoundedCornerShape(9.dp))
                            .clickable { onReceive() }
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
                } else {
                    Pill("إلغاء الطلب", Fv.Red) { onCancel() }
                }
            }
        }
    }
}

@Composable
private fun ItemPicker(
    products: List<Product>,
    query: String,
    onQuery: (String) -> Unit,
    onPick: (Product) -> Unit,
    onClose: () -> Unit,
) {
    val filtered = products.filter {
        query.isBlank() ||
            it.nameAr.contains(query, true) ||
            it.nameEn.contains(query, true) ||
            it.sku.contains(query, true)
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { onClose() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            color = Fv.Surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("اختر صنفًا", color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text("بحث بالاسم أو الرمز", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(filtered, key = { it.id }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(p) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nameAr, color = Fv.TextHigh, fontSize = 13.sp)
                                Text(p.sku, color = Fv.TextLow, fontSize = 11.sp)
                            }
                            Text("${p.vanStock}", color = Fv.TextMid, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitPicker(
    units: List<ProductUnit>,
    selected: ProductUnit?,
    onPick: (ProductUnit?) -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { onClose() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            color = Fv.Surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("الوحدة", color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                units.forEach { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(u) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                u.name,
                                color = if (u.id == selected?.id) Fv.Blue else Fv.TextHigh,
                                fontSize = 13.sp,
                                fontWeight = if (u.id == selected?.id) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                            if (u.conversionQty > 1) {
                                Text(
                                    "${u.conversionQty.toInt()} حبة",
                                    color = Fv.TextLow,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        // A variant unit has its own stock; a packaging unit
                        // draws on the item's base pool, where a number here
                        // would be the same figure repeated on every row.
                        if (u.isStockUnit) {
                            Text("${u.vanStock}", color = Fv.TextMid, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
