package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Print-paper palette (the captured image must be black-on-white for the thermal head).
private val PaperBg = Color.White
private val Ink = Color(0xFF111111)
private val InkMid = Color(0xFF555555)
private val Hair = Color(0xFFDDDDDD)

@Composable
fun ReceiptDetailScreen(
    paymentId: String,
    onBack: () -> Unit,
    viewModel: ReceiptDetailViewModel = koinViewModel { parametersOf(paymentId) },
) {
    val state by viewModel.state.collectAsState()
    val entity = state.entity
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    // Capture the on-screen receipt document and hand it to the VM as PNG bytes.
    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.print(png)
    }

    // Auto-print once a connection is established from the dialog.
    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (state.pendingPrint && state.printerState is PrinterState.Connected) captureAndPrint()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_back), contentDescription = null, tint = Fv.TextHigh, modifier = Modifier.size(22.dp))
                }
                Text(stringResource(Res.string.receipt_voucher_title), color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (entity != null) {
                    Surface(
                        onClick = {
                            if (state.printerState is PrinterState.Connected) scope.launch { captureAndPrint() }
                            else viewModel.requestConnectThenPrint()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Fv.Blue,
                    ) {
                        Text(
                            stringResource(Res.string.printer_thermal_print),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            state.printMessageAr?.let { msg ->
                Text(
                    msg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Fv.Amber, fontSize = 12.sp, textAlign = TextAlign.Center,
                )
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fv.Blue)
                }
                entity == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.receipt_detail_not_found), color = Fv.TextMid)
                }
                else -> Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(max = 340.dp)
                            .background(PaperBg, RoundedCornerShape(4.dp))
                            .drawWithContent {
                                graphicsLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(graphicsLayer)
                            },
                    ) {
                        PaymentReceiptDocument(entity, state)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = viewModel::connectTypeSelected,
                onAddressChanged = viewModel::connectAddressChanged,
                onDeviceSelected = viewModel::deviceSelected,
                onRefresh = viewModel::refreshDevices,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onDismiss = viewModel::dismissConnectDialog,
            )
        }
    }
}

/** The money-only cash / cheque receipt (سند قبض) — black on white for the printer. */
@Composable
private fun PaymentReceiptDocument(entity: PaymentEntity, state: ReceiptDetailState) {
    val methodLabel = when (entity.method) {
        "CASH" -> "نقدي"
        "CHEQUE" -> "شيك"
        "TRANSFER" -> "حوالة"
        else -> entity.method
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp)) {
        // Header
        Text(
            state.companyNameAr.ifBlank { "فان فلو" },
            modifier = Modifier.fillMaxWidth(),
            color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
        Text(
            "سند قبض",
            modifier = Modifier.fillMaxWidth(),
            color = InkMid, fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Thick()
        Spacer(Modifier.height(10.dp))

        DocRow("رقم السند", entity.number)
        DocRow("التاريخ", entity.createdAt.toDateTimeString())
        DocRow("العميل", state.customerNameAr.ifBlank { state.customerCode })
        if (state.salesmanNameAr.isNotBlank()) DocRow("المندوب", state.salesmanNameAr)
        DocRow("طريقة الدفع", methodLabel)

        Spacer(Modifier.height(10.dp))
        Dashed()
        Spacer(Modifier.height(10.dp))

        // Amount — the money
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المبلغ", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(entity.amount.formatJod(AppLanguage.AR), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Cheque details
        if (entity.method == "CHEQUE") {
            Spacer(Modifier.height(10.dp))
            Thick()
            Spacer(Modifier.height(8.dp))
            Text("بيانات الشيك", color = InkMid, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            entity.chequeBank?.let { DocRow("البنك", it) }
            entity.chequeNumber?.let { DocRow("رقم الشيك", it) }
            entity.chequeDate?.let { DocRow("تاريخ الاستحقاق", it.toDateString()) }
        }

        val transferRef = entity.transferRef
        if (entity.method == "TRANSFER" && !transferRef.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            DocRow("رقم المرجع", transferRef)
        }

        val notes = entity.notes
        if (!notes.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            DocRow("ملاحظة", notes)
        }

        Spacer(Modifier.height(14.dp))
        Dashed()
        Spacer(Modifier.height(10.dp))
        Text(
            "شكراً لتعاملكم معنا",
            modifier = Modifier.fillMaxWidth(),
            color = InkMid, fontSize = 11.sp, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DocRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = InkMid, fontSize = 12.sp)
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Dashed() {
    HorizontalDivider(color = Hair, thickness = 1.dp)
}

@Composable
private fun Thick() {
    HorizontalDivider(color = Ink, thickness = 3.dp)
}
