package com.jehadalomour.flowvan.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.fvFieldColors
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
import com.jehadalomour.flowvan.shared.presentation.feature.collection.CollectionEvent
import com.jehadalomour.flowvan.shared.presentation.feature.collection.CollectionViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.collection.JordanBanks
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(customerId) },
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
                Text("سند تحصيل", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { CustomerCard(state.customer) }
                item {
                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = { viewModel.onEvent(CollectionEvent.AmountChanged(it)) },
                        label = { Text("المبلغ (د.أ)", color = Fv.TextMid, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = fvFieldColors(),
                    )
                }
                if (state.advanceWarning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Fv.Amber.copy(alpha = 0.18f)),
                        ) {
                            Text(
                                "المبلغ أكبر من الرصيد — سيتم تسجيله كدفعة مقدمة",
                                color = Fv.Amber,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                item { MethodPicker(state.method) { viewModel.onEvent(CollectionEvent.MethodSelected(it)) } }
                if (state.method == PaymentMethod.CHEQUE) {
                    item {
                        OutlinedTextField(
                            value = state.chequeNumber,
                            onValueChange = { viewModel.onEvent(CollectionEvent.ChequeNumberChanged(it)) },
                            label = { Text("رقم الشيك", color = Fv.TextMid, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = fvFieldColors(),
                        )
                    }
                    item { BankPicker(state.chequeBank) { viewModel.onEvent(CollectionEvent.ChequeBankChanged(it)) } }
                }
                if (state.method == PaymentMethod.TRANSFER) {
                    item {
                        OutlinedTextField(
                            value = state.transferRef,
                            onValueChange = { viewModel.onEvent(CollectionEvent.TransferRefChanged(it)) },
                            label = { Text("رقم الحوالة", color = Fv.TextMid, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = fvFieldColors(),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onEvent(CollectionEvent.NotesChanged(it)) },
                        label = { Text("ملاحظات (اختياري)", color = Fv.TextMid, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = fvFieldColors(),
                    )
                }
            }
            Button(
                onClick = { viewModel.onEvent(CollectionEvent.Save) },
                enabled = (state.amount ?: 0.0) > 0 && !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fv.Amber,
                    contentColor = Fv.BgDeepest,
                    disabledContainerColor = Fv.SurfaceTop,
                    disabledContentColor = Fv.TextMid,
                ),
            ) { Text("حفظ التحصيل", fontWeight = FontWeight.Bold) }
        }
    }

    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CollectionEvent.DismissError) },
            title = { Text("خطأ", color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(CollectionEvent.DismissError) }) { Text("حسناً", color = Fv.Blue) }
            },
            containerColor = Fv.Surface,
        )
    }
}

@Composable
private fun CustomerCard(customer: com.jehadalomour.flowvan.shared.domain.model.Customer?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(customer?.nameAr ?: "...", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "الرصيد: ${(customer?.balance ?: 0.0).formatJod(AppLanguage.AR)}",
                color = if ((customer?.overdueAmount ?: 0.0) > 0) Fv.Red else Fv.TextMid,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MethodPicker(current: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PaymentMethod.CASH to "نقداً", PaymentMethod.CHEQUE to "شيك", PaymentMethod.TRANSFER to "تحويل")
            .forEach { (m, label) ->
                val active = m == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(m) }
                        .background(if (active) Fv.Blue else Fv.Surface, RoundedCornerShape(20.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) Fv.TextHigh else Fv.TextMid,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
    }
}

@Composable
private fun BankPicker(current: String, onSelect: (String) -> Unit) {
    Column {
        Text("البنك", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(JordanBanks) { bank ->
                val active = bank == current
                Box(
                    modifier = Modifier
                        .clickable { onSelect(bank) }
                        .background(if (active) Fv.Blue else Fv.Surface, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        bank,
                        color = if (active) Fv.TextHigh else Fv.TextMid,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
