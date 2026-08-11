package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.components.DateRangeBar
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.SummaryPill
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Force Latin (Western) digits even under a forced Arabic locale, matching the voucher receipts.
private val LtrNum = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

@Composable
fun VoucherSummaryScreen(
    onBack: () -> Unit,
    viewModel: VoucherSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()
    var showPrintPreview by remember { mutableStateOf(false) }

    // Capture the on-screen receipt preview and hand it to the ViewModel as PNG bytes.
    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(VoucherSummaryEvent.Print(png))
    }

    // Auto-print once a connection is established from the connect dialog.
    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (showPrintPreview && state.pendingPrint && state.printerState is PrinterState.Connected) {
            captureAndPrint()
        }
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
                Text(
                    stringResource(Res.string.voucher_summary_title),
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DateRangeBar(state.from, state.to) { f, t ->
                        viewModel.onEvent(VoucherSummaryEvent.SetFrom(f))
                        viewModel.onEvent(VoucherSummaryEvent.SetTo(t))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryPill(
                            stringResource(Res.string.all_sales_pill_sales),
                            state.totalSales.formatJod(AppLanguage.AR),
                            Fv.Blue,
                            Modifier.weight(1f),
                        )
                        SummaryPill(
                            stringResource(Res.string.all_sales_pill_returns),
                            state.totalReturns.formatJod(AppLanguage.AR),
                            Fv.Red,
                            Modifier.weight(1f),
                        )
                    }
                }
                if (state.rows.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.report_empty_period),
                            color = Fv.TextMid,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                } else {
                    items(state.rows, key = { it.id }) { row -> VoucherRow(row) }
                }
            }

            OutlinedButton(
                onClick = { showPrintPreview = true },
                enabled = state.rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    stringResource(Res.string.printer_thermal_print),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Fv.TextHigh,
                )
            }
        }
    }

    if (showPrintPreview) {
        VsPrintPreviewDialog(
            state = state,
            graphicsLayer = graphicsLayer,
            onThermalPrint = {
                if (state.printerState is PrinterState.Connected) {
                    scope.launch { captureAndPrint() }
                } else {
                    viewModel.onEvent(VoucherSummaryEvent.RequestConnectThenPrint)
                }
            },
            onSharePdf = {
                scope.launch {
                    val bmp = graphicsLayer.toImageBitmap()
                    pdfHelper.shareAsPdf(bmp, state.reportAt.toFileStamp())
                }
            },
            onClose = {
                viewModel.onEvent(VoucherSummaryEvent.DismissMessage)
                showPrintPreview = false
            },
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
private fun VoucherRow(row: VoucherSummaryRow) {
    val accent = if (row.type == "SALE") Fv.Blue else Fv.Red
    val typeLabel = if (row.type == "SALE") stringResource(Res.string.chip_sale) else stringResource(Res.string.chip_return)
    Surface(
        color = Fv.Surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.customerName.ifBlank { row.number },
                    color = Fv.TextHigh,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${row.number}  •  ${row.paymentType.labelAr}",
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                    style = LtrNum,
                )
            }
            Text(typeLabel, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp))
            Text(row.total.formatJod(AppLanguage.AR), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Print preview dialog (mirrors EndOfDayScreen / VoucherPrintScreen) ────────────────────────

private val PrDarkBlue = Color(0xFF1A2A3A)
private val PrScreenBg = Color(0xFFD1D5DB)
private val PrGreen    = Color(0xFF1D9E75)
private val PrSubText  = Color(0xFF637181)
private val PrDark     = Color(0xFF0F1923)

@Composable
private fun VsPrintPreviewDialog(
    state: VoucherSummaryState,
    graphicsLayer: GraphicsLayer,
    onThermalPrint: () -> Unit,
    onSharePdf: () -> Unit,
    onClose: () -> Unit,
    onEvent: (VoucherSummaryEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(PrScreenBg)) {
            // Top bar
            Surface(color = PrDarkBlue, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(Res.drawable.ic_back), contentDescription = null, tint = Color.White)
                    }
                    Text(
                        stringResource(Res.string.voucher_summary_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }

            // Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrActionButton(label = stringResource(Res.string.printer_thermal_print), filled = true, onClick = onThermalPrint)
                Spacer(Modifier.size(10.dp))
                PrActionButton(label = stringResource(Res.string.print_action_share_pdf), filled = false, onClick = onSharePdf)
            }

            // Printer status / last action feedback
            val printMessage = state.printMessageAr
            val statusMessage = when {
                state.isPrinting -> stringResource(Res.string.printer_printing)
                printMessage != null -> printMessage
                else -> printerStatusLabel(state.printerState)
            }
            Text(
                text = statusMessage,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = if (state.printerState is PrinterState.Connected) PrGreen else PrSubText,
            )

            // Receipt preview — the exact bitmap that gets rasterised and printed.
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .widthIn(max = 320.dp)
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        },
                ) {
                    VsReceiptBody(state)
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = { onEvent(VoucherSummaryEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { onEvent(VoucherSummaryEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { onEvent(VoucherSummaryEvent.DeviceSelected(it)) },
                onRefresh = { onEvent(VoucherSummaryEvent.RefreshDevices) },
                onConnect = { onEvent(VoucherSummaryEvent.Connect) },
                onDisconnect = { onEvent(VoucherSummaryEvent.Disconnect) },
                onDismiss = { onEvent(VoucherSummaryEvent.DismissConnectDialog) },
            )
        }
    }
}

@Composable
private fun PrActionButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) PrDarkBlue else Color.White,
        shadowElevation = if (filled) 2.dp else 0.dp,
        border = if (filled) null else BorderStroke(1.5.dp, Color(0xFFD1D5DB)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (filled) Color.White else PrDark,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun printerStatusLabel(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}

// ── Voucher summary receipt (monochrome, black-on-white — rasterised for the thermal head) ────

private val RcInk = Color.Black
private val RcMuted = Color(0xFF444444)
private val RcFaint = Color(0xFF888888)

@Composable
private fun VsReceiptBody(state: VoucherSummaryState) {
    val companyName = state.companyNameAr.ifBlank { state.companyNameEn }
    // The printed receipt is Arabic-first and always lays out right-to-left, regardless of the
    // device locale. Numbers stay LTR via the LtrNum text style.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {

        // Company header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The company's own logo (cached from /company-info) when available, else default.
            val logoBitmap = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp).padding(bottom = 8.dp),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.voucher_logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(RcInk),
                    modifier = Modifier.size(140.dp).padding(bottom = 8.dp),
                )
            }
            if (companyName.isNotBlank()) {
                Text(
                    text = companyName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = RcInk,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                )
            }
            val subLine = buildString {
                if (state.branch.isNotBlank()) append(state.branch)
                if (state.companyTaxNumber.isNotBlank()) {
                    if (isNotEmpty()) append(" | ")
                    append("الرقم الضريبي: ${state.companyTaxNumber}")
                }
            }
            if (subLine.isNotBlank()) {
                Text(
                    text = subLine,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RcMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                )
            }
        }

        RcSolid()

        // Title tag
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .border(2.dp, RcInk, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(Res.string.voucher_summary_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = RcInk,
                )
            }
        }

        RcDash()

        RcKv(stringResource(Res.string.from_date), state.from.toDateOnlyStr())
        RcKv(stringResource(Res.string.to_date), state.to.toDateOnlyStr())
        RcKv(stringResource(Res.string.print_salesman), state.salesmanNameAr)
        RcKv(stringResource(Res.string.voucher_summary_count), state.rows.size.toString())

        RcSolid()

        // Voucher rows
        state.rows.forEachIndexed { index, row ->
            if (index > 0) RcDash()
            val typeLabel = if (row.type == "SALE") stringResource(Res.string.chip_sale)
                else stringResource(Res.string.chip_return)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.customerName.ifBlank { row.number },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RcInk,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.total.formatJod(AppLanguage.AR),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RcInk,
                    style = LtrNum,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = row.number, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RcFaint, style = LtrNum)
                Text(text = "$typeLabel • ${row.paymentType.labelAr}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RcMuted)
            }
        }

        RcSolid()

        // Footer totals (6 lines: sales/returns split by cash/credit)
        RcKv(stringResource(Res.string.voucher_summary_total_sales), state.totalSales.formatJod(AppLanguage.AR), bold = true)
        RcKv(stringResource(Res.string.voucher_summary_total_returns), state.totalReturns.formatJod(AppLanguage.AR), bold = true)
        RcDash()
        RcKv(stringResource(Res.string.voucher_summary_cash_sales), state.cashSales.formatJod(AppLanguage.AR))
        RcKv(stringResource(Res.string.voucher_summary_cash_returns), state.cashReturns.formatJod(AppLanguage.AR))
        RcKv(stringResource(Res.string.voucher_summary_credit_sales), state.creditSales.formatJod(AppLanguage.AR))
        RcKv(stringResource(Res.string.voucher_summary_credit_returns), state.creditReturns.formatJod(AppLanguage.AR))

        RcSolid()

        // Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.print_footer_thanks), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = RcMuted)
            Spacer(Modifier.height(2.dp))
            Text("Powered by 7Software", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RcMuted, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
    }
}

@Composable
private fun RcKv(key: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = key, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RcMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
            color = RcInk,
            style = LtrNum,
        )
    }
}

@Composable
private fun RcSolid() {
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(RcInk, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RcDash() {
    Spacer(Modifier.height(6.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dash = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(RcFaint, Offset(x, 0f), Offset((x + dash).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dash + gap
        }
    }
    Spacer(Modifier.height(6.dp))
}

private fun Long.toDateOnlyStr(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/" +
        "${dt.year}"
}

/** Filesystem-safe stamp used to name the shared/printed PDF, e.g. "VSUM_20260710_1830". */
private fun Long.toFileStamp(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "VSUM_${dt.year}${dt.monthNumber.toString().padStart(2, '0')}${dt.dayOfMonth.toString().padStart(2, '0')}" +
        "_${dt.hour.toString().padStart(2, '0')}${dt.minute.toString().padStart(2, '0')}"
}
