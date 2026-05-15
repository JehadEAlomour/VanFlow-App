package com.jehadalomour.flowvan.screens.returns

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.CartLineRow
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.ProductPickerColumn
import com.jehadalomour.flowvan.screens.components.TotalsStrip
import com.jehadalomour.flowvan.screens.components.fvFieldColors
import com.jehadalomour.flowvan.shared.presentation.feature.returns.ReturnReason
import com.jehadalomour.flowvan.shared.presentation.feature.returns.ReturnVoucherEvent
import com.jehadalomour.flowvan.shared.presentation.feature.returns.ReturnVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReturnVoucherScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: ReturnVoucherViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.savedNumber) { if (state.savedNumber != null) onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    Text("فاتورة مرتجع", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    state.customer?.let { Text(it.nameAr, color = Fv.TextMid, fontSize = 11.sp) }
                }
                Box(
                    modifier = Modifier
                        .clickable { viewModel.onEvent(ReturnVoucherEvent.ToggleView) }
                        .background(Fv.SurfaceHigh, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (state.view == VoucherView.PICKER) "السلة (${state.cart.size})" else "المنتجات",
                        color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            when (state.view) {
                VoucherView.PICKER -> ProductPickerColumn(
                    products = state.visibleProducts,
                    searchQuery = state.searchQuery,
                    onSearch = { viewModel.onEvent(ReturnVoucherEvent.SearchChanged(it)) },
                    onAdd = { viewModel.onEvent(ReturnVoucherEvent.AddToCart(it)) },
                    showStockBadge = false,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
                VoucherView.CART -> LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.cart, key = { it.productId }) { line ->
                        CartLineRow(
                            line,
                            onChangeQty = { viewModel.onEvent(ReturnVoucherEvent.ChangeQty(line.productId, it)) },
                            onRemove = { viewModel.onEvent(ReturnVoucherEvent.RemoveLine(line.productId)) },
                        )
                    }
                    item { ReasonRow(state.reason) { viewModel.onEvent(ReturnVoucherEvent.ReasonSelected(it)) } }
                    item {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { viewModel.onEvent(ReturnVoucherEvent.NotesChanged(it)) },
                            label = { Text("ملاحظات (اختياري)", color = Fv.TextMid, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = fvFieldColors(),
                        )
                    }
                }
            }
            TotalsStrip(
                subtotal = state.subtotal,
                discount = null,
                tax = state.taxAmount,
                total = state.total,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Button(
                onClick = { viewModel.onEvent(ReturnVoucherEvent.OpenSaveSheet) },
                enabled = state.cart.isNotEmpty() && state.reason != null && !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fv.Red,
                    contentColor = Fv.TextHigh,
                    disabledContainerColor = Fv.SurfaceTop,
                    disabledContentColor = Fv.TextMid,
                ),
            ) { Text("حفظ المرتجع", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (state.showSaveSheet) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ReturnVoucherEvent.DismissSaveSheet) },
            title = { Text("تأكيد الحفظ", color = Fv.TextHigh) },
            text = { Text("سيتم تسجيل المرتجع وتعديل المخزون والرصيد", color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ReturnVoucherEvent.ConfirmSave) }) { Text("تأكيد", color = Fv.Red) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ReturnVoucherEvent.DismissSaveSheet) }) { Text("إلغاء", color = Fv.TextMid) }
            },
            containerColor = Fv.Surface,
        )
    }

    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ReturnVoucherEvent.DismissError) },
            title = { Text("خطأ", color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ReturnVoucherEvent.DismissError) }) { Text("حسناً", color = Fv.Blue) }
            },
            containerColor = Fv.Surface,
        )
    }
}

@Composable
private fun ReasonRow(selected: ReturnReason?, onSelect: (ReturnReason) -> Unit) {
    Column {
        Text("سبب الإرجاع *", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReturnReason.values().toList()) { reason ->
                val active = reason == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(reason) }
                        .background(if (active) Fv.Red else Fv.Surface, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        reason.labelAr,
                        color = if (active) Fv.TextHigh else Fv.TextMid,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
