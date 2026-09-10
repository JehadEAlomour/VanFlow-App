package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * الكشف اليومي on paper — the slip a rep hands over when closing a round.
 *
 * Same paper as the sales report, the statement and تقرير الحركات: same ink, same
 * type scale, same torn edges, so a shop or an office receiving any of them sees
 * one company's documents rather than four templates.
 *
 * What differs is the shape. The others list documents; this one answers a single
 * question — how much cash should be in the bag — and shows the three movements
 * that produced it. So it prints as totals, not a table, which also keeps it to a
 * short slip that a rep can hand over without a page of rows.
 */
@Composable
fun CashFlowPrintScreen(
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    viewModel: CashFlowPrintViewModel = koinViewModel {
        parametersOf(fromMillis, toMillis)
    },
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    val docName = remember(state.fromMillis) {
        "daily-cash-${state.fromMillis.txnDate().replace('/', '-')}"
    }

    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(CashFlowPrintEvent.Print(png))
    }

    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (state.pendingPrint && state.printerState is PrinterState.Connected) captureAndPrint()
    }

    Column(modifier = Modifier.fillMaxSize().background(TxnScreenBg)) {

        Surface(color = TxnDarkBlue, shadowElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_back), contentDescription = null, tint = Color.White)
                }
                Column {
                    Text(
                        stringResource(Res.string.cash_flow_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        "${state.fromMillis.txnDate()} - ${state.toMillis.txnDate()}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TxnBlue)
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TxnActionChip(
                label = stringResource(Res.string.printer_thermal_print),
                filled = true,
                onClick = {
                    if (state.printerState is PrinterState.Connected) {
                        scope.launch { captureAndPrint() }
                    } else {
                        viewModel.onEvent(CashFlowPrintEvent.RequestConnectThenPrint)
                    }
                },
            )
            Spacer(Modifier.size(10.dp))
            TxnActionChip(
                label = stringResource(Res.string.print_action_share_pdf),
                filled = false,
                onClick = {
                    scope.launch { pdfHelper.shareAsPdf(graphicsLayer.toImageBitmap(), docName) }
                },
            )
        }

        val statusMessage = when {
            state.isPrinting -> stringResource(Res.string.printer_printing)
            state.printMessageAr != null -> state.printMessageAr!!
            else -> txnPrinterStatusLabel(state.printerState)
        }
        Text(
            text = statusMessage,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = if (state.printerState is PrinterState.Connected) TxnGreen else TxnSubText,
        )

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = { viewModel.onEvent(CashFlowPrintEvent.ConnectTypeSelected(it)) },
                connectLanguage = state.connectLanguage,
                onLanguageSelected = { viewModel.onEvent(CashFlowPrintEvent.PrinterLanguageSelected(it)) },
                onAddressChanged = { viewModel.onEvent(CashFlowPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(CashFlowPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(CashFlowPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(CashFlowPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(CashFlowPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(CashFlowPrintEvent.DismissConnectDialog) },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = TxnPaperWidth)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(TxnPaperBg, RoundedCornerShape(4.dp))
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                Column {
                    TxnTear()
                    CashFlowBody(state)
                    TxnTear(flipped = true)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

@Composable
private fun CashFlowBody(state: CashFlowPrintState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    // Untinted — a JPEG logo tinted with the ink colour prints as a
                    // solid black rectangle. See StatementPrintScreen.
                    Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(TxnLogoSize))
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.size(TxnLogoSize),
                        colorFilter = ColorFilter.tint(TxnInk),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            TxnCenter(state.companyNameAr, TXN_FS_COMPANY, bold = true)
            if (state.companyNameEn.isNotBlank()) TxnCenter(state.companyNameEn, TXN_FS_SUB)
            if (state.companyTaxNumber.isNotBlank()) {
                TxnCenter(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    TXN_FS_SUB,
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(TxnInk).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.cash_flow_title),
                    color = TxnPaperBg,
                    fontSize = TXN_FS_TITLE.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (state.salesmanNameAr.isNotBlank()) {
                TxnInfo(stringResource(Res.string.statement_salesman), state.salesmanNameAr)
            }
            TxnInfo(
                stringResource(Res.string.txn_report_period),
                "${state.fromMillis.txnDate()} - ${state.toMillis.txnDate()}",
            )
            TxnInfo(stringResource(Res.string.statement_printed_at), state.printedAt.txnDateTime())

            val nothingHappened = state.salesCount == 0 &&
                state.returnsCount == 0 &&
                state.collectionsCount == 0

            if (nothingHappened) {
                Spacer(Modifier.height(10.dp))
                TxnRule()
                Spacer(Modifier.height(10.dp))
                TxnCenter(stringResource(Res.string.cash_flow_empty), TXN_FS_INFO)
                Spacer(Modifier.height(10.dp))
            } else {
                CashFlowSection(
                    title = stringResource(Res.string.cash_flow_section_sales),
                    firstLabel = stringResource(Res.string.cash_flow_sales_cash),
                    firstValue = state.salesCashTotal,
                    secondLabel = stringResource(Res.string.cash_flow_sales_credit),
                    secondValue = state.salesCreditTotal,
                    totalLabel = stringResource(Res.string.all_sales_total_sales),
                    totalValue = state.salesTotal,
                    count = state.salesCount,
                )
                CashFlowSection(
                    title = stringResource(Res.string.cash_flow_section_returns),
                    firstLabel = stringResource(Res.string.cash_flow_returns_cash),
                    firstValue = state.returnsCashTotal,
                    secondLabel = stringResource(Res.string.cash_flow_returns_credit),
                    secondValue = state.returnsCreditTotal,
                    totalLabel = stringResource(Res.string.all_sales_total_returns),
                    totalValue = state.returnsTotal,
                    count = state.returnsCount,
                )
                CashFlowSection(
                    title = stringResource(Res.string.cash_flow_section_collections),
                    firstLabel = stringResource(Res.string.cash_flow_collection_cash),
                    firstValue = state.collectionsCashTotal,
                    secondLabel = stringResource(Res.string.cash_flow_collection_cheque),
                    secondValue = state.collectionsChequeTotal,
                    totalLabel = stringResource(Res.string.cash_flow_print_collections_total),
                    totalValue = state.collectionsTotal,
                    count = state.collectionsCount,
                )
            }

            Spacer(Modifier.height(8.dp))
            // The line the handover turns on: cash sales + cash collected, less any
            // cash refunded. Boxed because it is the number being counted against.
            TxnBoxedTotal(
                stringResource(Res.string.cash_flow_total_cash),
                state.totalCash.txnJod(),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** One movement of the day: its two halves, its total, and how many documents made it. */
@Composable
private fun CashFlowSection(
    title: String,
    firstLabel: String,
    firstValue: Double,
    secondLabel: String,
    secondValue: Double,
    totalLabel: String,
    totalValue: Double,
    count: Int,
) {
    Spacer(Modifier.height(8.dp))
    TxnRule()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = TXN_FS_HEAD.sp, fontWeight = FontWeight.Bold, color = TxnInk)
    }
    TxnRule()
    Spacer(Modifier.height(4.dp))
    TxnInfo(firstLabel, firstValue.txnJod())
    TxnInfo(secondLabel, secondValue.txnJod())
    TxnInfo(stringResource(Res.string.cash_flow_print_count), count.toString())
    TxnThinRule()
    TxnTotal(totalLabel, totalValue.txnJod())
}
