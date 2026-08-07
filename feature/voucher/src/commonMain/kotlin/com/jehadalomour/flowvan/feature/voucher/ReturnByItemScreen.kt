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
import org.koin.compose.viewmodel.koinViewModel

/**
 * Return by item — the rep picks what is coming back, the server says which
 * sales it came from.
 *
 * The match is shown before anything is created, because the customer is
 * standing there: "which invoice?" has an answer on screen rather than a phone
 * call to the office.
 */
@Composable
fun ReturnByItemScreen(
    onBack: () -> Unit,
    onDone: (vouchers: List<String>) -> Unit,
    viewModel: ReturnByItemViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Fv.Surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "إرجاع بالصنف",
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "رجوع",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "اختر الأصناف والكميات، ثم اعرض المطابقة — سيحدد النظام فواتير البيع التي خرجت منها.",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                )
            }

            itemsIndexed(state.lines) { i, line ->
                ReturnLineCard(
                    line = line,
                    onQty = { viewModel.onEvent(ReturnByItemEvent.QuantityChanged(i, it)) },
                    onRemove = { viewModel.onEvent(ReturnByItemEvent.RemoveLine(i)) },
                )
            }

            item {
                Surface(
                    onClick = { viewModel.onEvent(ReturnByItemEvent.OpenPicker) },
                    shape = RoundedCornerShape(10.dp),
                    color = Fv.SurfaceTop,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+ إضافة صنف",
                            color = Fv.Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            state.errorAr?.let { msg ->
                item {
                    Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                        Text(
                            msg,
                            color = Fv.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                                .clickable { viewModel.onEvent(ReturnByItemEvent.DismissError) },
                        )
                    }
                }
            }

            // ── The match ────────────────────────────────────────────────────
            state.plan?.let { plan ->
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text(
                        "المطابقة: ${plan.voucherCount} سند · ${fmt3(plan.refundTotal)}",
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(plan.lines) { l ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Fv.Surface) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(l.itemName, color = Fv.TextHigh, fontSize = 13.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "من ${l.voucherNumber} · ${l.inDate.take(16).replace('T', ' ')}",
                                color = Fv.TextMid,
                                fontSize = 12.sp,
                            )
                            Text(
                                "${fmt3(l.quantity)} × ${fmt3(l.unitPrice)} = ${fmt3(l.netTotal)}",
                                color = Fv.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Never hidden: a customer handing over 10 when only 7 are
                // returnable must hear it now, not after the paperwork.
                if (plan.unallocated.isNotEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    "تعذّرت مطابقة:",
                                    color = Fv.Amber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                plan.unallocated.forEach { u ->
                                    Text(
                                        "${u.itemNumber} × ${fmt3(u.quantity)}",
                                        color = Fv.TextMid,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.createdVouchers.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(10.dp), color = Fv.SurfaceTop) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "✓ تم إنشاء ${state.createdVouchers.size} سند إرجاع",
                                color = Fv.Green,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                state.createdVouchers.joinToString("، "),
                                color = Fv.TextMid,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                item {
                    PrimaryButton("تم", enabled = true) { onDone(state.createdVouchers) }
                }
            } else {
                item {
                    PrimaryButton(
                        label = if (state.plan == null) "عرض المطابقة" else "تأكيد الإرجاع",
                        enabled = if (state.plan == null) state.canPreview else state.canConfirm,
                        loading = state.isPreviewing || state.isConfirming,
                    ) {
                        viewModel.onEvent(
                            if (state.plan == null) {
                                ReturnByItemEvent.Preview
                            } else {
                                ReturnByItemEvent.Confirm
                            },
                        )
                    }
                }
            }
        }
    }

    if (state.isPickerOpen) {
        ProductPickerSheet(
            products = state.products.filter {
                state.searchQuery.isBlank() ||
                    it.nameAr.contains(state.searchQuery, true) ||
                    it.sku.contains(state.searchQuery, true)
            },
            query = state.searchQuery,
            onQuery = { viewModel.onEvent(ReturnByItemEvent.SearchChanged(it)) },
            onPick = { viewModel.onEvent(ReturnByItemEvent.AddProduct(it)) },
            onClose = { viewModel.onEvent(ReturnByItemEvent.ClosePicker) },
        )
    }
}

@Composable
private fun ReturnLineCard(
    line: ReturnByItemLine,
    onQty: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = Fv.Surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.product.nameAr, color = Fv.TextHigh, fontSize = 13.sp)
                Text(
                    line.unitLabel ?: line.product.unit,
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                )
            }
            OutlinedTextField(
                value = line.quantity,
                onValueChange = onQty,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(90.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Fv.SurfaceTop,
                    unfocusedContainerColor = Fv.SurfaceTop,
                    focusedTextColor = Fv.TextHigh,
                    unfocusedTextColor = Fv.TextHigh,
                    cursorColor = Fv.Blue,
                    focusedIndicatorColor = Fv.Blue,
                    unfocusedIndicatorColor = Fv.SurfaceTop,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "حذف",
                color = Fv.Red,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onRemove),
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled && !loading) onClick() },
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Fv.Blue else Fv.SurfaceTop,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    label,
                    color = if (enabled) Color.White else Fv.TextMid,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Three decimals without relying on platform String.format (KMP). */
private fun fmt3(v: Double): String {
    val scaled = kotlin.math.round(v * 1000).toLong()
    val whole = scaled / 1000
    val frac = kotlin.math.abs(scaled % 1000).toString().padStart(3, '0')
    return "$whole.$frac"
}

/**
 * Item picker. A full-screen overlay rather than a modal sheet: the rep is
 * one-handed in a shop, and the list needs the whole screen to be scannable.
 */
@Composable
private fun ProductPickerSheet(
    products: List<com.jehadalomour.flowvan.core.model.Product>,
    query: String,
    onQuery: (String) -> Unit,
    onPick: (com.jehadalomour.flowvan.core.model.Product) -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = Fv.BgDeepest, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "اختر صنفًا",
                    color = Fv.TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "إغلاق",
                    color = Fv.TextMid,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onClose),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                placeholder = { Text("ابحث بالاسم أو الرمز", color = Fv.TextMid, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Fv.Surface,
                    unfocusedContainerColor = Fv.Surface,
                    focusedTextColor = Fv.TextHigh,
                    unfocusedTextColor = Fv.TextHigh,
                    cursorColor = Fv.Blue,
                    focusedIndicatorColor = Fv.Blue,
                    unfocusedIndicatorColor = Fv.SurfaceTop,
                ),
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products) { p ->
                    Surface(
                        onClick = { onPick(p) },
                        shape = RoundedCornerShape(10.dp),
                        color = Fv.Surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(p.nameAr, color = Fv.TextHigh, fontSize = 13.sp)
                            Text(p.sku, color = Fv.TextMid, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
