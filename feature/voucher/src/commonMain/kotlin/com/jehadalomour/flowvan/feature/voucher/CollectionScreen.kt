package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.fvFieldColors
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.feature.voucher.ChequeEntry
import com.jehadalomour.flowvan.feature.voucher.CollectionEvent
import com.jehadalomour.flowvan.feature.voucher.CollectionViewModel
import com.jehadalomour.flowvan.feature.voucher.JordanBank
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_alarm
import com.jehadalomour.flowvan.core.designsystem.resources.ic_back
import com.jehadalomour.flowvan.core.designsystem.resources.ic_cancel
import com.jehadalomour.flowvan.core.designsystem.resources.ic_payment
import com.jehadalomour.flowvan.core.designsystem.resources.ic_receipt
import com.jehadalomour.flowvan.core.designsystem.resources.ic_warning
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    customerId: String,
    onBack: () -> Unit,
    onSaved: (paymentId: String) -> Unit = { onBack() },
    viewModel: CollectionViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    // After a successful save, open the printable receipt for the collection.
    LaunchedEffect(state.savedPaymentId) {
        val pid = state.savedPaymentId
        if (pid != null) onSaved(pid)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                Text(
                    stringResource(Res.string.collection_voucher_title),
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Customer hero card
                item { CustomerHeroCard(state.customer) }

                // Amount section — only for CASH and TRANSFER
                if (state.method != PaymentMethod.CHEQUE) {
                    item {
                        AmountBox(
                            amountText = state.amountText,
                            onAmountChange = { viewModel.onEvent(CollectionEvent.AmountChanged(it)) },
                            onQuickFill = { viewModel.onEvent(CollectionEvent.QuickFillAmount(it)) },
                        )
                    }
                }

                // Advance warning
                if (state.advanceWarning) {
                    item { AdvanceWarningBanner() }
                }

                // Method picker
                item {
                    MethodPicker(
                        current = state.method,
                        onSelect = { viewModel.onEvent(CollectionEvent.MethodSelected(it)) },
                    )
                }

                // Cheque cards
                if (state.method == PaymentMethod.CHEQUE) {
                    itemsIndexed(state.cheques) { idx, cheque ->
                        ChequeCard(
                            index = idx,
                            cheque = cheque,
                            canRemove = state.cheques.size > 1,
                            onAmountChange = { viewModel.onEvent(CollectionEvent.ChequeAmountChanged(idx, it)) },
                            onNumberChange = { viewModel.onEvent(CollectionEvent.ChequeNumberChanged(idx, it)) },
                            onDateChange = { viewModel.onEvent(CollectionEvent.ChequeDateChanged(idx, it)) },
                            onOpenBank = { viewModel.onEvent(CollectionEvent.OpenBankSheet(idx)) },
                            onRemove = { viewModel.onEvent(CollectionEvent.RemoveCheque(idx)) },
                        )
                    }
                    item {
                        AddChequeButton { viewModel.onEvent(CollectionEvent.AddCheque) }
                    }
                    // Cheque total summary
                    val totalAmount = state.cheques.sumOf { it.amount ?: 0.0 }
                    if (totalAmount > 0) {
                        item {
                            ChequeTotalRow(total = totalAmount, advanceWarning = state.advanceWarning)
                        }
                    }
                }

                // Transfer ref
                if (state.method == PaymentMethod.TRANSFER) {
                    item {
                        OutlinedTextField(
                            value = state.transferRef,
                            onValueChange = { viewModel.onEvent(CollectionEvent.TransferRefChanged(it)) },
                            label = { Text(stringResource(Res.string.collection_transfer_ref), color = Fv.TextMid, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = fvFieldColors(),
                        )
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onEvent(CollectionEvent.NotesChanged(it)) },
                        label = { Text(stringResource(Res.string.collection_notes_optional), color = Fv.TextMid, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        colors = fvFieldColors(),
                    )
                }

                item { Spacer(Modifier.height(4.dp)) }
            }

            // Save button
            SaveButton(
                enabled = (state.amount ?: 0.0) > 0 && !state.isSaving,
                saving = state.isSaving,
                onClick = { viewModel.onEvent(CollectionEvent.Save) },
            )
        }
    }

    // Bank picker bottom sheet
    if (state.bankSheetOpenForIndex != null) {
        BankBottomSheet(
            banks = state.filteredBanks,
            query = state.bankSearchQuery,
            onQueryChange = { viewModel.onEvent(CollectionEvent.BankSearchQueryChanged(it)) },
            onSelect = { viewModel.onEvent(CollectionEvent.BankSelected(it)) },
            onDismiss = { viewModel.onEvent(CollectionEvent.CloseBankSheet) },
        )
    }

    // Error dialog
    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CollectionEvent.DismissError) },
            title = { Text(stringResource(Res.string.error_title), color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(CollectionEvent.DismissError) }) {
                    Text(stringResource(Res.string.ok), color = Fv.Blue)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

// ── Customer hero card ──────────────────────────────────────────────────────

@Composable
private fun CustomerHeroCard(customer: com.jehadalomour.flowvan.core.model.Customer?) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF1E4FBF), Color(0xFF2C6FE4)))
    val balance = customer?.balance ?: 0.0
    val overdue = customer?.overdueAmount ?: 0.0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                customer?.nameAr ?: "...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                customer?.code ?: "",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BalanceChip(
                    label = stringResource(Res.string.balance_label),
                    value = balance.formatJod(AppLanguage.AR),
                    valueColor = Color.White,
                )
                if (overdue > 0) {
                    BalanceChip(
                        label = stringResource(Res.string.overdue_label),
                        value = overdue.formatJod(AppLanguage.AR),
                        valueColor = Color(0xFFFFCDD2),
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceChip(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Amount box ──────────────────────────────────────────────────────────────

@Composable
private fun AmountBox(
    amountText: String,
    onAmountChange: (String) -> Unit,
    onQuickFill: (Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(Res.string.collection_amount_jod), color = Fv.TextMid, fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fv.TextHigh,
                    textAlign = TextAlign.End,
                ),
            )
            // Quick fill row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25.0, 50.0, 100.0, 200.0).forEach { amt ->
                    QuickFillChip(
                        label = stringResource(Res.string.amount_jod_format, amt.toInt()),
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickFill(amt) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickFillChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Fv.Blue.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Fv.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Advance warning ─────────────────────────────────────────────────────────

@Composable
private fun AdvanceWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Fv.Amber.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = Fv.Amber,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(Res.string.collection_advance_warning_short),
            color = Fv.Amber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Method picker ───────────────────────────────────────────────────────────

@Composable
private fun MethodPicker(current: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(PaymentMethod.CASH, Res.drawable.ic_payment, stringResource(Res.string.collection_method_cash)),
            Triple(PaymentMethod.CHEQUE, Res.drawable.ic_receipt, stringResource(Res.string.collection_method_cheque)),
            Triple(PaymentMethod.TRANSFER, Res.drawable.ic_payment, stringResource(Res.string.chip_transfer)),
        ).forEach { (method, iconRes, label) ->
            val active = method == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Fv.Blue else Fv.Surface)
                    .border(
                        width = 1.dp,
                        color = if (active) Fv.Blue else Fv.Border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(method) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = if (active) Color.White else Fv.TextMid,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        label,
                        color = if (active) Color.White else Fv.TextMid,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ── Cheque card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChequeCard(
    index: Int,
    cheque: ChequeEntry,
    canRemove: Boolean,
    onAmountChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onDateChange: (Long?) -> Unit,
    onOpenBank: () -> Unit,
    onRemove: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Fv.Blue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            color = Fv.Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        stringResource(Res.string.cheque_number_format, index + 1),
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_cancel),
                            contentDescription = stringResource(Res.string.delete),
                            tint = Fv.Red,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = cheque.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(Res.string.collection_amount_jod), color = Fv.TextMid, fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )

            // Cheque number
            OutlinedTextField(
                value = cheque.number,
                onValueChange = onNumberChange,
                label = { Text(stringResource(Res.string.collection_cheque_number), color = Fv.TextMid, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )

            // Bank picker trigger
            BankTriggerField(bank = cheque.bank, onClick = onOpenBank)

            // Date picker trigger
            DateTriggerField(
                dateMillis = cheque.dateMillis,
                onClick = { showDatePicker = true },
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = cheque.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(pickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(Res.string.confirm), color = Fv.Blue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun BankTriggerField(bank: JordanBank?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Fv.Surface)
            .border(1.dp, Fv.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (bank != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(bank.colorHex)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(bank.initial, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(bank.nameAr, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Text(stringResource(Res.string.collection_select_bank), color = Fv.TextMid, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DateTriggerField(dateMillis: Long?, onClick: () -> Unit) {
    val label = dateMillis?.let { millis ->
        val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        "${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
    } ?: stringResource(Res.string.collection_cheque_date_optional)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Fv.Surface)
            .border(1.dp, Fv.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (dateMillis != null) Fv.TextHigh else Fv.TextMid,
                fontSize = 13.sp,
            )
            Icon(
                painter = painterResource(Res.drawable.ic_alarm),
                contentDescription = null,
                tint = Fv.TextLow,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddChequeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Fv.Blue.copy(alpha = 0.07f))
            .border(1.dp, Fv.Blue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.collection_add_cheque),
            color = Fv.Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChequeTotalRow(total: Double, advanceWarning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (advanceWarning) Fv.Amber.copy(alpha = 0.08f) else Fv.Green.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.collection_cheques_total), color = Fv.TextMid, fontSize = 13.sp)
        Text(
            total.formatJod(AppLanguage.AR),
            color = if (advanceWarning) Fv.Amber else Fv.Green,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Bank bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankBottomSheet(
    banks: List<JordanBank>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (JordanBank) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Fv.Surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.collection_select_bank), color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(Res.string.collection_search_bank), color = Fv.TextLow) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(320.dp),
            ) {
                items(banks) { bank ->
                    BankGridItem(bank = bank, onClick = { onSelect(bank) })
                }
            }
        }
    }
}

@Composable
private fun BankGridItem(bank: JordanBank, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Fv.BgDeepest)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(bank.colorHex)),
            contentAlignment = Alignment.Center,
        ) {
            Text(bank.initial, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            bank.nameAr,
            color = Fv.TextHigh,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Save button ─────────────────────────────────────────────────────────────

@Composable
private fun SaveButton(enabled: Boolean, saving: Boolean, onClick: () -> Unit) {
    val gradient = if (enabled) {
        Brush.linearGradient(listOf(Color(0xFF0FA968), Color(0xFF0B8050)))
    } else {
        Brush.linearGradient(listOf(Fv.SurfaceTop, Fv.SurfaceTop))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(gradient)
            .then(if (enabled && !saving) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (saving) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                stringResource(Res.string.collection_save_button),
                color = if (enabled) Color.White else Fv.TextMid,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
