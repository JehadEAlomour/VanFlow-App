package com.jehadalomour.flowvan.screens.reports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.presentation.feature.receiptdetail.ReceiptDetailViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReceiptDetailScreen(
    paymentId: String,
    onBack: () -> Unit,
    viewModel: ReceiptDetailViewModel = koinViewModel { parametersOf(paymentId) },
) {
    val state by viewModel.state.collectAsState()
    val entity = state.entity

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
                Text(stringResource(Res.string.receipt_voucher_title), color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fv.Blue)
                }
                entity == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.receipt_detail_not_found), color = Fv.TextMid)
                }
                else -> ReceiptContent(entity)
            }
        }
    }
}

@Composable
private fun ReceiptContent(entity: PaymentEntity) {
    val (methodLabel, methodColor) = when (entity.method) {
        "CASH" -> stringResource(Res.string.method_cash_label) to Fv.Green
        "CHEQUE" -> stringResource(Res.string.method_cheque_label) to Fv.Amber
        "TRANSFER" -> stringResource(Res.string.method_transfer_label) to Fv.Blue
        else -> entity.method to Fv.TextMid
    }
    val (statusLabel, statusColor) = when (entity.status) {
        "CONFIRMED" -> stringResource(Res.string.receipt_status_confirmed) to Fv.Green
        "BOUNCED" -> stringResource(Res.string.receipt_status_bounced) to Fv.Red
        else -> stringResource(Res.string.payment_status_pending) to Fv.Amber
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Fv.Surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorBadge(methodLabel, methodColor)
                        Spacer(Modifier.width(8.dp))
                        ColorBadge(statusLabel, statusColor, alpha = 0.12f)
                        Spacer(Modifier.weight(1f))
                        Text(entity.number, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Fv.SurfaceHigh)
                    ReceiptRow(stringResource(Res.string.receipt_detail_date), entity.createdAt.toDateTimeString())
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Fv.Surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.receipt_detail_amount), color = Fv.TextMid, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(entity.amount.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(methodLabel, color = methodColor, fontSize = 13.sp)
                }
            }
        }

        if (entity.method == "CHEQUE") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.receipt_detail_cheque_info), color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider(color = Fv.SurfaceHigh)
                        entity.chequeNumber?.let { ReceiptRow(stringResource(Res.string.collection_cheque_number), it) }
                        entity.chequeBank?.let { ReceiptRow(stringResource(Res.string.collection_bank), it) }
                        entity.chequeDate?.let { ReceiptRow(stringResource(Res.string.collection_cheque_date), it.toDateString()) }
                    }
                }
            }
        }

        val transferRef = entity.transferRef
        if (entity.method == "TRANSFER" && transferRef != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.receipt_detail_transfer_info), color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider(color = Fv.SurfaceHigh)
                        ReceiptRow(stringResource(Res.string.receipt_detail_ref_number), transferRef)
                    }
                }
            }
        }

        val notes = entity.notes
        if (!notes.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(Res.string.receipt_detail_notes), color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(notes, color = Fv.TextHigh, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
