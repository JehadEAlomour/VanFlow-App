package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.feature.print.PrinterConnectDialog
import com.jehadalomour.flowvan.feature.print.rememberPdfShareHelper
import com.jehadalomour.flowvan.feature.print.toPngBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// Force Latin (Western) digits even under a forced Arabic locale, where ASCII digits would
// otherwise shape as Arabic-Indic (٠١٢…). Matches the invoice voucher's number handling.
private val LtrNum = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

@Composable
fun EndOfDayScreen(
    onLoggedOut: () -> Unit,
    viewModel: EndOfDayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()
    var showPrintPreview by remember { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) onLoggedOut() }

    // Capture the on-screen receipt preview and hand it to the ViewModel as PNG bytes.
    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(EndOfDayEvent.Print(png))
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
                Icon(
                    painter = painterResource(Res.drawable.ic_moon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(start = 8.dp),
                )
                Text(
                    stringResource(Res.string.home_quick_end_of_day),
                    color = Fv.TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                state.kpi?.let { kpi ->
                    SectionCard(title = stringResource(Res.string.end_of_day_summary_title)) {
                        KpiRow(stringResource(Res.string.end_of_day_total_sales), kpi.salesTotal.formatJod(AppLanguage.AR), Fv.Green)
                        KpiRow(stringResource(Res.string.end_of_day_cash_sales), kpi.cashSalesTotal.formatJod(AppLanguage.AR), Fv.Green)
                        KpiRow(stringResource(Res.string.end_of_day_credit_sales), kpi.creditSalesTotal.formatJod(AppLanguage.AR), Fv.Amber)
                        KpiRow(stringResource(Res.string.end_of_day_total_returns), kpi.returnsTotal.formatJod(AppLanguage.AR), Fv.Red)
                        HorizontalDivider(color = Fv.SurfaceHigh, modifier = Modifier.padding(vertical = 4.dp))
                        KpiRow(stringResource(Res.string.end_of_day_net_sales), (kpi.salesTotal - kpi.returnsTotal).formatJod(AppLanguage.AR), Fv.TextHigh)
                        KpiRow(stringResource(Res.string.end_of_day_total_collections), kpi.collectionsTotal.formatJod(AppLanguage.AR), Fv.Amber)
                        Spacer(Modifier.height(4.dp))
                        KpiRow(
                            stringResource(Res.string.end_of_day_customers_visited),
                            "${kpi.customersVisited} / ${kpi.customersPlanned}",
                            if (kpi.customersVisited >= kpi.customersPlanned) Fv.Green else Fv.Amber,
                        )
                    }
                }

                SectionCard(title = stringResource(Res.string.end_of_day_cash_settlement)) {
                    KpiRow(stringResource(Res.string.method_cash_label), state.cashCollectedToday.formatJod(AppLanguage.AR), Fv.Green)
                    KpiRow(stringResource(Res.string.end_of_day_cheques), state.chequesCollectedToday.formatJod(AppLanguage.AR), Fv.Blue)
                    KpiRow(stringResource(Res.string.end_of_day_transfers), state.transfersCollectedToday.formatJod(AppLanguage.AR), Fv.Teal)
                }

                SectionCard(title = stringResource(Res.string.end_of_day_sync_status)) {
                    KpiRow(
                        stringResource(Res.string.end_of_day_unsynced_invoices),
                        state.unsyncedInvoices.toString(),
                        if (state.unsyncedInvoices == 0) Fv.Green else Fv.Amber,
                    )
                    KpiRow(
                        stringResource(Res.string.end_of_day_unsynced_payments),
                        state.unsyncedPayments.toString(),
                        if (state.unsyncedPayments == 0) Fv.Green else Fv.Amber,
                    )
                    if (state.unsyncedInvoices > 0 || state.unsyncedPayments > 0) {
                        Text(
                            stringResource(Res.string.end_of_day_sync_note),
                            color = Fv.TextMid,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Print summary — opens the print preview dialog (same as the voucher print screen).
            OutlinedButton(
                onClick = { showPrintPreview = true },
                enabled = state.kpi != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(48.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    stringResource(Res.string.end_of_day_print_summary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Fv.TextHigh,
                )
            }

            Button(
                onClick = { viewModel.onEvent(EndOfDayEvent.OpenConfirmDialog) },
                enabled = !state.isEnding,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fv.Red,
                    contentColor = Fv.TextHigh,
                    disabledContainerColor = Fv.SurfaceTop,
                    disabledContentColor = Fv.TextMid,
                ),
            ) {
                Text(
                    if (state.isEnding) stringResource(Res.string.end_of_day_ending) else stringResource(Res.string.end_of_day_end_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }

    if (showPrintPreview) {
        EodPrintPreviewDialog(
            state = state,
            graphicsLayer = graphicsLayer,
            onThermalPrint = {
                if (state.printerState is PrinterState.Connected) {
                    scope.launch { captureAndPrint() }
                } else {
                    viewModel.onEvent(EndOfDayEvent.RequestConnectThenPrint)
                }
            },
            onSystemPrint = {
                scope.launch {
                    val bmp = graphicsLayer.toImageBitmap()
                    pdfHelper.printDocument(bmp, state.reportAt.toFileStamp())
                }
            },
            onSharePdf = {
                scope.launch {
                    val bmp = graphicsLayer.toImageBitmap()
                    pdfHelper.shareAsPdf(bmp, state.reportAt.toFileStamp())
                }
            },
            onClose = {
                viewModel.onEvent(EndOfDayEvent.DismissMessage)
                showPrintPreview = false
            },
            onEvent = viewModel::onEvent,
        )
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EndOfDayEvent.DismissConfirmDialog) },
            title = { Text(stringResource(Res.string.end_of_day_confirm_end_title), color = Fv.TextHigh) },
            text = { Text(stringResource(Res.string.end_of_day_confirm_end_message), color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EndOfDayEvent.ConfirmEndShift) }) {
                    Text(stringResource(Res.string.confirm), color = Fv.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EndOfDayEvent.DismissConfirmDialog) }) {
                    Text(stringResource(Res.string.cancel), color = Fv.TextMid)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

// ── Print preview dialog (mirrors VoucherPrintScreen: action bar + receipt preview) ──────────

private val PrDarkBlue = Color(0xFF1A2A3A)
private val PrScreenBg = Color(0xFFD1D5DB)
private val PrGreen    = Color(0xFF1D9E75)
private val PrSubText  = Color(0xFF637181)
private val PrDark     = Color(0xFF0F1923)

@Composable
private fun EodPrintPreviewDialog(
    state: EndOfDayState,
    graphicsLayer: GraphicsLayer,
    onThermalPrint: () -> Unit,
    onSystemPrint: () -> Unit,
    onSharePdf: () -> Unit,
    onClose: () -> Unit,
    onEvent: (EndOfDayEvent) -> Unit,
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
                        stringResource(Res.string.end_of_day_receipt_title),
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
                PrActionButton(label = stringResource(Res.string.print_action_print), filled = false, onClick = onSystemPrint)
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
                    EodReceiptBody(state)
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
                onTypeSelected = { onEvent(EndOfDayEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { onEvent(EndOfDayEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { onEvent(EndOfDayEvent.DeviceSelected(it)) },
                onRefresh = { onEvent(EndOfDayEvent.RefreshDevices) },
                onConnect = { onEvent(EndOfDayEvent.Connect) },
                onDisconnect = { onEvent(EndOfDayEvent.Disconnect) },
                onDismiss = { onEvent(EndOfDayEvent.DismissConnectDialog) },
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
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun KpiRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Fv.TextMid, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun printerStatusLabel(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}

// ── EOD receipt (monochrome, black-on-white — rasterised for the 1-bit thermal head) ─────────

private val RcInk = Color.Black
private val RcMuted = Color(0xFF444444)
private val RcFaint = Color(0xFF888888)

@Composable
private fun EodReceiptBody(state: EndOfDayState) {
    val companyName = state.companyNameAr.ifBlank { state.companyNameEn }
    // The printed receipt is Arabic-first and must always lay out right-to-left, no matter the
    // device/app locale — otherwise labels and their values would flip when printed under an LTR
    // locale. Numbers stay LTR via the LtrNum text style.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {

        // Company header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.voucher_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(RcInk),
                modifier = Modifier.size(54.dp).padding(bottom = 4.dp),
            )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .border(2.dp, RcInk, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(Res.string.end_of_day_receipt_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = RcInk,
                )
            }
        }

        RcDash()

        RcKv(stringResource(Res.string.voucher_detail_date), state.reportAt.toReceiptDateStr())
        RcKv(stringResource(Res.string.print_salesman), state.salesmanNameAr)

        RcSolid()

        // Summary
        RcSectionLabel(stringResource(Res.string.end_of_day_summary_title))
        state.kpi?.let { kpi ->
            RcKv(stringResource(Res.string.end_of_day_total_sales), kpi.salesTotal.formatJod(AppLanguage.AR))
            RcKv(stringResource(Res.string.end_of_day_cash_sales), kpi.cashSalesTotal.formatJod(AppLanguage.AR))
            RcKv(stringResource(Res.string.end_of_day_credit_sales), kpi.creditSalesTotal.formatJod(AppLanguage.AR))
            RcKv(stringResource(Res.string.end_of_day_total_returns), kpi.returnsTotal.formatJod(AppLanguage.AR))
            RcKv(stringResource(Res.string.end_of_day_net_sales), (kpi.salesTotal - kpi.returnsTotal).formatJod(AppLanguage.AR), bold = true)
            RcKv(stringResource(Res.string.end_of_day_total_collections), kpi.collectionsTotal.formatJod(AppLanguage.AR))
            RcKv(stringResource(Res.string.end_of_day_customers_visited), "${kpi.customersVisited} / ${kpi.customersPlanned}")
        }

        RcDash()

        // Cash settlement
        RcSectionLabel(stringResource(Res.string.end_of_day_cash_settlement))
        RcKv(stringResource(Res.string.method_cash_label), state.cashCollectedToday.formatJod(AppLanguage.AR))
        RcKv(stringResource(Res.string.end_of_day_cheques), state.chequesCollectedToday.formatJod(AppLanguage.AR))
        RcKv(stringResource(Res.string.end_of_day_transfers), state.transfersCollectedToday.formatJod(AppLanguage.AR))

        RcDash()

        // Sync status
        RcSectionLabel(stringResource(Res.string.end_of_day_sync_status))
        RcKv(stringResource(Res.string.end_of_day_unsynced_invoices), state.unsyncedInvoices.toString())
        RcKv(stringResource(Res.string.end_of_day_unsynced_payments), state.unsyncedPayments.toString())

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
private fun RcSectionLabel(label: String) {
    Text(
        text = label,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = RcInk,
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
    )
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
    Spacer(Modifier.height(7.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dash = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(RcFaint, Offset(x, 0f), Offset((x + dash).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dash + gap
        }
    }
    Spacer(Modifier.height(7.dp))
}

private fun Long.toReceiptDateStr(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/" +
        "${dt.year}  " +
        "${dt.hour.toString().padStart(2, '0')}:" +
        dt.minute.toString().padStart(2, '0')
}

/** Filesystem-safe stamp used to name the shared/printed PDF, e.g. "EOD_20260710_1830". */
private fun Long.toFileStamp(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "EOD_${dt.year}${dt.monthNumber.toString().padStart(2, '0')}${dt.dayOfMonth.toString().padStart(2, '0')}" +
        "_${dt.hour.toString().padStart(2, '0')}${dt.minute.toString().padStart(2, '0')}"
}
