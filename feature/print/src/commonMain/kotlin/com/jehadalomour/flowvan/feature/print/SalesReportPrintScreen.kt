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
 * edges on screen — because a shop and an office receiving all three should see one
 * company's documents, not three templates. What differs is the columns: this one lists
 * the round's vouchers across customers and settles two questions, what was sold net of
 * returns and how much of it went out on credit.
 *
 * The paper is drawn twice: once for the user to look at, and once off-screen through
 * [ThermalCapture] at the head's own resolution. Everything inside [SalesReportBody] is
 * therefore 1-bit work — black or white, no grey, no alpha, nothing under 16sp — while
 * the chrome around it stays an ordinary screen.
 */
@Composable
fun SalesReportPrintScreen(
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    onOpenBulk: (Long, Long) -> Unit = { _, _ -> },
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
            Spacer(Modifier.size(10.dp))
            // Print each SALE invoice's own detail slip with its tax QR.
            TxnActionChip(
                label = stringResource(Res.string.bulk_print_title),
                filled = false,
                onClick = { onOpenBulk(state.fromMillis, state.toMillis) },
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
                connectLanguage = state.connectLanguage,
                onLanguageSelected = { viewModel.onEvent(SalesReportPrintEvent.PrinterLanguageSelected(it)) },
                onAddressChanged = { viewModel.onEvent(SalesReportPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(SalesReportPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(SalesReportPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(SalesReportPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(SalesReportPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(SalesReportPrintEvent.DismissConnectDialog) },
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
                    SalesReportBody(state)
                    TxnTear(flipped = true)
                }
            }

            // The print source: the same body, drawn off-screen at the head's own
            // resolution. Pinning the density is what makes 384dp of paper 576 dots on
            // the Sunmi terminal as well as on a modern phone — previously the terminal
            // handed the printer a third of the detail and nothing downstream could add
            // it back. The torn edges are left out on purpose: they are grey strips, and
            // grey is the one thing a 1-bit head cannot print.
            ThermalCapture(layer = graphicsLayer, paperDp = TxnPaperWidth) {
                SalesReportBody(state)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

/**
 * How wide a logo is allowed to be on the paper — 110dp of 384, not the 300dp this
 * used to be. A customer's upload is a continuous-tone photograph whatever we do to
 * it, and a photograph reaches a 1-bit head as dithered speckle; the only lever left
 * is how much of the receipt that speckle is allowed to cover. Small enough, and the
 * company name is what gets read at the top of the page.
 */
private val SalesPaperLogo = 110.dp

/**
 * Rules, drawn here instead of through the kit's hairlines.
 *
 * A rule on 1-bit paper cannot be made quieter by fading it — [TxnThinRule] is half a
 * dp at 35% alpha, which is the least printable mark a thermal head can be handed. The
 * only honest lever is thickness, so both weights are solid black: 3dp closes a
 * section, 2dp separates two vouchers.
 */
@Composable
private fun SalesPaperRule(thick: Boolean = false) {
    Box(
        Modifier.fillMaxWidth()
            .height(if (thick) 3.dp else ThermalInk.RuleThickness)
            .background(ThermalInk.Ink),
    )
}

// Column widths, re-budgeted for the larger type. A three-decimal figure is about
// 80dp at this size, so the credit column — which used to be the narrowest because it
// is usually just a dash — takes what the date and the movement label can spare.
private const val SALES_COL_DATE = 0.95f
private const val SALES_COL_DOC = 0.95f
private const val SALES_COL_TOTAL = 1.25f
private const val SALES_COL_CREDIT = 1.20f

@Composable
private fun SalesReportBody(state: SalesReportPrintState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    // Untinted — a JPEG logo tinted with the ink colour prints as a
                    // solid black rectangle. See StatementPrintScreen. Tinting would not
                    // have made it 1-bit anyway: a tint multiplies colour and leaves the
                    // alpha channel, so the soft edges still arrive as partial coverage.
                    Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(SalesPaperLogo))
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.size(SalesPaperLogo),
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
            // because both halves of it are already 1-bit.
            Box(
                modifier = Modifier.fillMaxWidth().background(ThermalInk.Ink).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.all_sales_title),
                    color = ThermalInk.Paper,
                    fontSize = ThermalInk.FS_TITLE.sp,
                    fontWeight = FontWeight.ExtraBold,
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
            SalesPaperRule(thick = true)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TxnHead(stringResource(Res.string.txn_report_col_date), SALES_COL_DATE)
                TxnHead(stringResource(Res.string.txn_report_col_doc), SALES_COL_DOC)
                TxnHead(stringResource(Res.string.txn_report_col_total), SALES_COL_TOTAL)
                TxnHead(stringResource(Res.string.txn_report_col_credit), SALES_COL_CREDIT)
            }
            SalesPaperRule(thick = true)

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                TxnCenter(stringResource(Res.string.report_empty_period), ThermalInk.FS_ROW)
                Spacer(Modifier.height(10.dp))
            } else {
                state.rows.forEach { row ->
                    SalesPaperRow(row)
                    SalesPaperRule()
                }
            }

            Spacer(Modifier.height(6.dp))
            SalesPaperRule(thick = true)
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
            SalesPaperRule(thick = true)
            Spacer(Modifier.height(6.dp))

            TxnInfo(stringResource(Res.string.statement_printed_at), state.printedAt.txnDateTime())

            Spacer(Modifier.height(12.dp))
            TxnCenter(stringResource(Res.string.print_footer_thanks), ThermalInk.FS_MIN)
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
            TxnCell(row.dateMillis.txnDate().take(5), SALES_COL_DATE)
            TxnCell(label, SALES_COL_DOC)
            TxnCell(row.total.txnJod(), SALES_COL_TOTAL, bold = true)
            TxnCell(if (row.isCredit) row.total.txnJod() else "-", SALES_COL_CREDIT)
        }
        // Whose voucher it is, then the number left-to-right — a rep reconciling this
        // against the office reads down the shop names, not the numbers. That order used
        // to be carried by nothing at all (both lines were 12sp Bold); at print sizes it
        // is carried by the name being a step larger and a weight heavier. The name may
        // take a second line: a shop whose name is cut in half is worse than a wrap.
        if (row.customerNameAr.isNotBlank()) {
            Text(
                text = row.customerNameAr,
                modifier = Modifier.fillMaxWidth(),
                color = ThermalInk.Ink,
                fontSize = ThermalInk.FS_ROW.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
