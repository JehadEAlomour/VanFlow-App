package com.jehadalomour.flowvan.screens.sale

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
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherEvent
import com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.sale.VoucherView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SaleVoucherScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: SaleVoucherViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedNumber) {
        if (state.savedNumber != null) onBack()
    }

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
                        onAdd = { viewModel.onEvent(SaleVoucherEvent.AddToCart(it)) },
                        showStockBadge = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    )
                }
                VoucherView.CART -> CartView(
                    state = state,
                    onChangeQty = { id, q -> viewModel.onEvent(SaleVoucherEvent.ChangeQty(id, q)) },
                    onRemove = { viewModel.onEvent(SaleVoucherEvent.RemoveLine(it)) },
                    onDiscountChange = { viewModel.onEvent(SaleVoucherEvent.DiscountChanged(it)) },
                    onNotesChange = { viewModel.onEvent(SaleVoucherEvent.NotesChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
            }

            TotalsStrip(
                subtotal = state.subtotal,
                discount = state.discountAmount.takeIf { it > 0 },
                tax = state.taxAmount,
                total = state.total,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Fv.SurfaceHigh, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (view == VoucherView.PICKER) "السلة ($count)" else "المنتجات",
            color = Fv.TextHigh,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CartView(
    state: com.jehadalomour.flowvan.shared.presentation.feature.sale.SaleVoucherState,
    onChangeQty: (String, Double) -> Unit,
    onRemove: (String) -> Unit,
    onDiscountChange: (Double) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.cart, key = { it.productId }) { line ->
            CartLineRow(line, onChangeQty = { onChangeQty(line.productId, it) }, onRemove = { onRemove(line.productId) })
        }
        item {
            OutlinedTextField(
                value = if (state.discountAmount == 0.0) "" else state.discountAmount.toString(),
                onValueChange = { onDiscountChange(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("خصم إجمالي (د.أ)", color = Fv.TextMid, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
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
                    PaymentMethod.CREDIT to "آجل",
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
