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
import androidx.compose.ui.text.style.TextOverflow
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
 * The printable/shareable تقرير المبيعات.
 *
 * Same paper as the statement and تقرير الحركات — same ink, same type scale, same torn
 * edges — because a shop and an office receiving all three should see one company's
 * documents, not three templates. What differs is the columns: this one lists the
 * round's vouchers across customers and settles two questions, what was sold net of
 * returns and how much of it went out on credit.
 */
@Composable
fun SalesReportPrintScreen(
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    viewModel: SalesReportPrintViewModel = koinViewModel {
        parametersOf(fromMillis, toMillis)
    },
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    val docName = remember(state.fromMillis, state.toMillis) {
        "sales-report-${state.fromMillis.txnDate().replace('/', '-')}"
    }

    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(SalesReportPrintEvent.Print(png))
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
                        stringResource(Res.string.all_sales_title),
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
                        viewModel.onEvent(SalesReportPrintEvent.RequestConnectThenPrint)
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
                onTypeSelected = { viewModel.onEvent(SalesReportPrintEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { viewModel.onEvent(SalesReportPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(SalesReportPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(SalesReportPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(SalesReportPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(SalesReportPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(SalesReportPrintEvent.DismissConnectDialog) },
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
                    SalesReportBody(state)
                    TxnTear(flipped = true)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

@Composable
private fun SalesReportBody(state: SalesReportPrintState) {
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
                    stringResource(Res.string.all_sales_title),
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
            TxnInfo(stringResource(Res.string.all_sales_count), state.count.toString())

            Spacer(Modifier.height(8.dp))
            TxnRule()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TxnHead(stringResource(Res.string.txn_report_col_date), 0.95f)
                TxnHead(stringResource(Res.string.txn_report_col_doc), 1.05f)
                TxnHead(stringResource(Res.string.txn_report_col_total), 1.35f)
                TxnHead(stringResource(Res.string.txn_report_col_credit), 0.95f)
            }
            TxnRule()

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                TxnCenter(stringResource(Res.string.report_empty_period), TXN_FS_INFO)
                Spacer(Modifier.height(10.dp))
            } else {
                state.rows.forEach { row ->
                    SalesPaperRow(row)
                    TxnThinRule()
                }
            }

            Spacer(Modifier.height(6.dp))
            TxnRule()
            Spacer(Modifier.height(6.dp))

            TxnTotal(stringResource(Res.string.all_sales_total_sales), state.salesTotal.txnJod())
            TxnTotal(stringResource(Res.string.all_sales_total_returns), state.returnsTotal.txnJod())
            if (state.requestsTotal > 0.0) {
                TxnTotal(stringResource(Res.string.all_sales_total_requests), state.requestsTotal.txnJod())
            }
            TxnTotal(stringResource(Res.string.txn_report_total_cash), state.cashTotal.txnJod())

            Spacer(Modifier.height(6.dp))
            // The two figures the round is closed on: what was earned, and how much of
            // it has not been paid for yet.
            TxnBoxedTotal(stringResource(Res.string.all_sales_pill_net), state.netTotal.txnJod())
            Spacer(Modifier.height(4.dp))
            TxnBoxedTotal(stringResource(Res.string.txn_report_total_credit), state.creditTotal.txnJod())

            Spacer(Modifier.height(10.dp))
            TxnRule()
            Spacer(Modifier.height(6.dp))

            TxnInfo(stringResource(Res.string.statement_printed_at), state.printedAt.txnDateTime())

            Spacer(Modifier.height(12.dp))
            TxnCenter(stringResource(Res.string.print_footer_thanks), TXN_FS_FOOTER)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SalesPaperRow(row: SalesReportPrintRow) {
    val label = when (row.type) {
        "SALE" -> stringResource(Res.string.print_voucher_type_sale)
        "RETURN" -> stringResource(Res.string.print_voucher_type_return)
        else -> stringResource(Res.string.print_voucher_type_request)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TxnCell(row.dateMillis.txnDate().take(5), 0.95f)
            TxnCell(label, 1.05f)
            TxnCell(row.total.txnJod(), 1.35f, bold = true)
            TxnCell(if (row.isCredit) row.total.txnJod() else "-", 0.95f)
        }
        // Whose voucher it is, then the number left-to-right — a rep reconciling this
        // against the office reads down the shop names, not the numbers.
        if (row.customerNameAr.isNotBlank()) {
            Text(
                text = row.customerNameAr,
                modifier = Modifier.fillMaxWidth(),
                color = TxnInk,
                fontSize = TXN_FS_SUB_ROW.sp,
                fontWeight = TxnWeight,
                textAlign = TextAlign.Right,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = row.number,
            modifier = Modifier.fillMaxWidth(),
            color = TxnInk,
            fontSize = TXN_FS_SUB_ROW.sp,
            fontWeight = TxnWeight,
            style = TxnLtr,
            textAlign = TextAlign.Left,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
