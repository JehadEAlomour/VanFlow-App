package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.data.repository.CustomerTxn
import com.jehadalomour.flowvan.core.data.repository.TxnKind
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
 * The printable/shareable تقرير الحركات.
 *
 * Shares the statement's paper design deliberately — same ink, same type scale,
 * same torn edges — because a shop receiving both should see two documents from
 * one company, not two templates. What differs is what the columns say: this one
 * lists movement and splits it cash/credit; the statement runs a balance.
 *
 * The paper is drawn twice: once for the user to look at, and once off-screen
 * through [ThermalCapture] at the head's own resolution. Everything inside
 * [TxnReportBody] is therefore 1-bit work — black or white, no grey, no alpha,
 * nothing under 16sp — while the chrome around it stays an ordinary screen.
 */
@Composable
fun TxnReportPrintScreen(
    customerId: String,
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    viewModel: TxnReportPrintViewModel = koinViewModel {
        parametersOf(customerId, fromMillis, toMillis)
    },
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    val docName = remember(state.customerCode) {
        "txn-report-${state.customerCode.ifBlank { "customer" }}"
    }

    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(TxnReportPrintEvent.Print(png))
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
                        stringResource(Res.string.txn_report_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    if (state.customerNameAr.isNotBlank()) {
                        Text(state.customerNameAr, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TxnBlue)
            }
            return@Column
        }

        // No paper is drawn at all when the server could not be reached. Printing
        // a report that silently omits half the movement is the failure this
        // whole screen exists to avoid.
        state.errorAr?.let { message ->
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(message, color = TxnAmber, fontSize = 14.sp, textAlign = TextAlign.Center)
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
                        viewModel.onEvent(TxnReportPrintEvent.RequestConnectThenPrint)
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
                onTypeSelected = { viewModel.onEvent(TxnReportPrintEvent.ConnectTypeSelected(it)) },
                connectLanguage = state.connectLanguage,
                onLanguageSelected = { viewModel.onEvent(TxnReportPrintEvent.PrinterLanguageSelected(it)) },
                onAddressChanged = { viewModel.onEvent(TxnReportPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(TxnReportPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(TxnReportPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(TxnReportPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(TxnReportPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(TxnReportPrintEvent.DismissConnectDialog) },
            )
        }

        // The paper is a FIXED width — see requiredWidth below — so a screen
        // narrower than it pans rather than clipping a column off the edge.
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Preview only. It draws at the phone's density, keeps the shadow and the
            // torn edges, and records nothing — what gets printed is the capture below.
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .requiredWidth(TxnPaperWidth)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(TxnPaperBg, RoundedCornerShape(4.dp)),
            ) {
                Column {
                    TxnTear()
                    TxnReportBody(state)
                    TxnTear(flipped = true)
                }
            }

            // The print source, and the PDF's source too: the same body, drawn
            // off-screen at the head's own resolution. Pinning the density is what makes
            // 384dp of paper 576 dots on the Sunmi terminal as well as on a modern phone
            // — the terminal was handing the printer a third of the detail, and nothing
            // downstream could add back dots that had never been drawn. The torn edges
            // are left out on purpose: they are decoration, and a sawtooth strip is not
            // worth the rows of ink it costs on a roll.
            ThermalCapture(layer = graphicsLayer, paperDp = TxnPaperWidth) {
                TxnReportBody(state)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

/**
 * How wide a logo is allowed to be on the paper — 110dp of 384, where this used to
 * draw 300. A customer's upload is a continuous-tone photograph whatever we do to it,
 * and a photograph reaches a 1-bit head as dithered speckle; the only lever left is
 * how much of the page that speckle is allowed to cover. Small enough, and the company
 * name is what gets read at the top of the receipt.
 *
 * Kept here rather than taken from the kit, as the sales report and the cash slip keep
 * theirs: how much of a page goes to a picture is that page's own decision.
 */
private val TxnReportLogo = 110.dp

/**
 * Rules, drawn here instead of through the kit's pair.
 *
 * A rule on 1-bit paper cannot be made quieter by fading it — [TxnThinRule] used to be
 * half a dp at 35% alpha, the least printable mark a head can be handed — and now that
 * the kit draws both of its rules at the same printable 2dp, nothing is left to tell a
 * table boundary from a row separator. The only honest lever is thickness, so both
 * weights here are solid black: 3dp opens and closes the table, 2dp separates one
 * movement from the next.
 */
@Composable
private fun TxnReportRule(thick: Boolean = false) {
    Box(
        Modifier.fillMaxWidth()
            .height(if (thick) 3.dp else ThermalInk.RuleThickness)
            .background(ThermalInk.Ink),
    )
}

// Column widths, re-budgeted for the larger type and kept identical to the sales
// report's, so the two tables read as the same table. A three-decimal figure is about
// 80dp at this size, so the money columns take what the date — five characters — and
// the movement label can spare.
private const val TXN_COL_DATE = 0.95f
private const val TXN_COL_DOC = 0.95f
private const val TXN_COL_TOTAL = 1.25f
private const val TXN_COL_CREDIT = 1.20f

@Composable
private fun TxnReportBody(state: TxnReportPrintState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    // Untinted — see StatementPrintScreen: a JPEG logo tinted with
                    // the ink colour prints as a solid black rectangle. Tinting would
                    // not have made it 1-bit anyway: a tint multiplies colour and leaves
                    // the alpha channel, so soft edges still arrive as partial coverage.
                    Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(TxnReportLogo))
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.size(TxnReportLogo),
                        colorFilter = ColorFilter.tint(ThermalInk.Ink),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            TxnCenter(state.companyNameAr, ThermalInk.FS_COMPANY, bold = true)
            if (state.companyNameEn.isNotBlank()) TxnCenter(state.companyNameEn, ThermalInk.FS_SUB)
            if (state.companyTaxNumber.isNotBlank()) {
                TxnCenter(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    ThermalInk.FS_SUB,
                )
            }

            Spacer(Modifier.height(8.dp))
            // Black band, white knockout — the one filled panel the paper is allowed,
            // because both halves of it are already 1-bit. Square corners: a rounded one
            // is a grey arc.
            Box(
                modifier = Modifier.fillMaxWidth().background(ThermalInk.Ink).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.txn_report_title),
                    color = ThermalInk.Paper,
                    fontSize = ThermalInk.FS_TITLE.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(8.dp))

            TxnInfo(stringResource(Res.string.statement_customer), state.customerNameAr)
            if (state.customerCode.isNotBlank()) {
                TxnInfo(stringResource(Res.string.statement_customer_code), state.customerCode)
            }
            if (state.customerPhone.isNotBlank()) {
                TxnInfo(stringResource(Res.string.statement_phone), state.customerPhone)
            }
            TxnInfo(
                stringResource(Res.string.txn_report_period),
                "${state.fromMillis.txnDate()} - ${state.toMillis.txnDate()}",
            )

            Spacer(Modifier.height(8.dp))
            TxnReportRule(thick = true)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TxnHead(stringResource(Res.string.txn_report_col_date), TXN_COL_DATE)
                TxnHead(stringResource(Res.string.txn_report_col_doc), TXN_COL_DOC)
                TxnHead(stringResource(Res.string.txn_report_col_total), TXN_COL_TOTAL)
                TxnHead(stringResource(Res.string.txn_report_col_credit), TXN_COL_CREDIT)
            }
            TxnReportRule(thick = true)

            if (state.report.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                TxnCenter(stringResource(Res.string.txn_report_empty), ThermalInk.FS_ROW)
                Spacer(Modifier.height(10.dp))
            } else {
                state.report.rows.forEach { row ->
                    TxnPaperRow(row)
                    TxnReportRule()
                }
            }

            Spacer(Modifier.height(6.dp))
            TxnReportRule(thick = true)
            Spacer(Modifier.height(6.dp))

            TxnTotal(stringResource(Res.string.all_sales_total_sales), state.report.salesTotal.txnJod())
            TxnTotal(stringResource(Res.string.all_sales_total_returns), state.report.returnsTotal.txnJod())
            TxnTotal(stringResource(Res.string.txn_report_total_collections), state.report.collectionsTotal.txnJod())
            TxnTotal(stringResource(Res.string.txn_report_total_cash), state.report.cashTotal.txnJod())

            Spacer(Modifier.height(6.dp))
            // The two figures the report is opened to settle: what moved, and how
            // much of it is still owed for.
            TxnBoxedTotal(stringResource(Res.string.txn_report_net_total), state.report.netTotal.txnJod())
            Spacer(Modifier.height(4.dp))
            TxnBoxedTotal(stringResource(Res.string.txn_report_total_credit), state.report.creditTotal.txnJod())

            Spacer(Modifier.height(10.dp))
            TxnReportRule(thick = true)
            Spacer(Modifier.height(6.dp))

            if (state.salesmanNameAr.isNotBlank()) {
                TxnInfo(stringResource(Res.string.statement_salesman), state.salesmanNameAr)
            }
            TxnInfo(stringResource(Res.string.statement_printed_at), state.printedAt.txnDateTime())

            Spacer(Modifier.height(12.dp))
            TxnCenter(stringResource(Res.string.print_footer_thanks), ThermalInk.FS_MIN)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TxnPaperRow(row: CustomerTxn) {
    val label = when (row.kind) {
        TxnKind.SALE -> stringResource(Res.string.print_voucher_type_sale)
        TxnKind.RETURN -> stringResource(Res.string.print_voucher_type_return)
        TxnKind.ORDER -> stringResource(Res.string.print_voucher_type_request)
        TxnKind.COLLECTION -> stringResource(Res.string.txn_report_collection)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TxnCell(row.date.takeLast(5), TXN_COL_DATE)
            TxnCell(label, TXN_COL_DOC)
            TxnCell(row.total.txnJod(), TXN_COL_TOTAL, bold = true)
            TxnCell(if (row.credit > 0) row.credit.txnJod() else "-", TXN_COL_CREDIT)
        }
        // Document number on its own line, left to right — see the statement. It sits at
        // the floor of the scale rather than above it: it is the one thing on the row a
        // reader looks up rather than reads, and the movement above it has to stay the
        // larger of the two.
        Text(
            text = row.number,
            modifier = Modifier.fillMaxWidth(),
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_MIN.sp,
            fontWeight = FontWeight.Bold,
            style = TxnLtr,
            textAlign = TextAlign.Left,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
