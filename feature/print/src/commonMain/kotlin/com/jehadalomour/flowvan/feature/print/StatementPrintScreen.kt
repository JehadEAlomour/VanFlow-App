package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs

// ── Design tokens ────────────────────────────────────────────────────────────
// Deliberately the same values as the voucher receipt: the two papers come off
// the same roll minutes apart, and a shopkeeper comparing them should not be
// able to tell they were built by different screens.

private val PaperBg  = Color.White
private val ScreenBg = Color(0xFFD1D5DB)
private val DarkBlue = Color(0xFF1A2A3A)
private val Blue     = Color(0xFF185FA5)
private val Green    = Color(0xFF1D9E75)
private val SubText  = Color(0xFF637181)
private val TearGray = Color(0xFFD1D5DB)

// Pure black ink. Hierarchy comes from weight, size and rules — never colour:
// a 1-bit thermal head dithers any grey into a stipple, and the Jordan rollout
// standardised on monochrome paper.
private val Ink = Color.Black

// Force Western digits and LTR on every number, so ASCII digits do not shape as
// Arabic-Indic (٠١٢…) under an Arabic locale and a balance stays readable.
private val LtrNum = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

/** Statement money: always 3 decimals, always Latin digits, sign carried separately. */
private fun Double.jod(): String {
    val v = abs(this)
    val whole = v.toLong()
    val frac = ((v - whole) * 1000).toLong().coerceIn(0, 999)
    return "$whole.${frac.toString().padStart(3, '0')}"
}

private fun Long.dayMonth(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.monthNumber.toString().padStart(2, '0')}"
}

private fun Long.fullDate(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    return "$d/$m/${dt.year}"
}

private fun Long.fullDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "${fullDate()} $h:$min"
}

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * The printable/shareable customer account statement.
 *
 * Same three exits as the voucher receipt — thermal roll, the OS print dialog,
 * and share-as-PDF — because a statement is settled in three different ways: on
 * the spot at the counter, filed in the office, or sent to the shopkeeper's
 * WhatsApp when he is not there.
 */
@Composable
fun StatementPrintScreen(
    customerId: String,
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    viewModel: StatementPrintViewModel = koinViewModel {
        parametersOf(customerId, fromMillis, toMillis)
    },
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    // The document name used by the share sheet and the OS print dialog.
    val docName = remember(state.customerCode, state.toMillis) {
        "statement-${state.customerCode.ifBlank { "customer" }}"
    }

    // Capturing the on-screen paper is the one thing that cannot live in the VM;
    // everything after the PNG does.
    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(StatementPrintEvent.Print(png))
    }

    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (state.pendingPrint && state.printerState is PrinterState.Connected) {
            captureAndPrint()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBg)) {

        Surface(color = DarkBlue, shadowElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Column {
                    Text(
                        stringResource(Res.string.statement_title),
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
                CircularProgressIndicator(color = Blue)
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
            ActionChip(
                label = stringResource(Res.string.printer_thermal_print),
                filled = true,
                onClick = {
                    if (state.printerState is PrinterState.Connected) {
                        scope.launch { captureAndPrint() }
                    } else {
                        viewModel.onEvent(StatementPrintEvent.RequestConnectThenPrint)
                    }
                },
            )
            Spacer(Modifier.size(10.dp))
            ActionChip(
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
            else -> printerStatusLabel(state.printerState)
        }
        Text(
            text = statusMessage,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = if (state.printerState is PrinterState.Connected) Green else SubText,
        )

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = { viewModel.onEvent(StatementPrintEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { viewModel.onEvent(StatementPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(StatementPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(StatementPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(StatementPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(StatementPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(StatementPrintEvent.DismissConnectDialog) },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 320.dp)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(PaperBg, RoundedCornerShape(4.dp))
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                Column {
                    PaperTear()
                    StatementBody(state)
                    PaperTear(flipped = true)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

@Composable
private fun StatementBody(state: StatementPrintState) {
    // The paper is laid out RTL like the rest of the app, but every numeric
    // column inside is pinned LTR so the digits read left-to-right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = null,
                        modifier = Modifier.height(38.dp),
                        colorFilter = ColorFilter.tint(Ink),
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.height(34.dp),
                        colorFilter = ColorFilter.tint(Ink),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            CenterLine(state.companyNameAr, size = 14, bold = true)
            if (state.companyNameEn.isNotBlank()) CenterLine(state.companyNameEn, size = 10)
            if (state.companyTaxNumber.isNotBlank()) {
                CenterLine(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    size = 10,
                )
            }

            Spacer(Modifier.height(8.dp))
            // The document's own name, boxed so it cannot be mistaken for an invoice.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Ink)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.statement_title),
                    color = PaperBg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Who and when ──────────────────────────────────────────────────
            InfoLine(stringResource(Res.string.statement_customer), state.customerNameAr)
            if (state.customerCode.isNotBlank()) {
                InfoLine(stringResource(Res.string.statement_customer_code), state.customerCode, numeric = true)
            }
            if (state.customerPhone.isNotBlank()) {
                InfoLine(stringResource(Res.string.statement_phone), state.customerPhone, numeric = true)
            }
            InfoLine(
                stringResource(Res.string.statement_period),
                "${state.fromMillis.fullDate()} - ${state.toMillis.fullDate()}",
                numeric = true,
            )

            Spacer(Modifier.height(8.dp))
            Rule()
            Spacer(Modifier.height(6.dp))

            // ── Opening balance ───────────────────────────────────────────────
            TotalLine(
                label = stringResource(Res.string.statement_opening_balance),
                value = state.openingBalance.jod(),
                bold = false,
            )

            Spacer(Modifier.height(6.dp))
            Rule()

            // ── Column headings ───────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                HeadCell(stringResource(Res.string.statement_col_date), weight = 1.0f)
                HeadCell(stringResource(Res.string.statement_col_doc), weight = 1.5f)
                HeadCell(stringResource(Res.string.statement_col_debit), weight = 1.1f, end = true)
                HeadCell(stringResource(Res.string.statement_col_credit), weight = 1.1f, end = true)
                HeadCell(stringResource(Res.string.statement_col_balance), weight = 1.2f, end = true)
            }
            Rule()

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                CenterLine(stringResource(Res.string.statement_empty), size = 11)
                Spacer(Modifier.height(10.dp))
            } else {
                state.rows.forEach { row ->
                    StatementPaperRow(row)
                    ThinRule()
                }
            }

            Spacer(Modifier.height(6.dp))
            Rule()
            Spacer(Modifier.height(6.dp))

            // ── Totals ────────────────────────────────────────────────────────
            TotalLine(stringResource(Res.string.statement_debits), state.totalDebits.jod())
            TotalLine(stringResource(Res.string.statement_credits), state.totalCredits.jod())

            Spacer(Modifier.height(6.dp))
            // The number the whole page exists to state, boxed so the eye lands
            // on it before anything else.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Ink)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(Res.string.statement_closing_balance),
                        color = PaperBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.closingBalance.jod(),
                        color = PaperBg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        style = LtrNum,
                    )
                }
            }

            // A negative closing balance means the shop is in credit — rare, and
            // exactly the case someone will otherwise read as a debt.
            if (state.closingBalance < 0) {
                Spacer(Modifier.height(4.dp))
                CenterLine(stringResource(Res.string.statement_in_credit), size = 10, bold = true)
            }

            Spacer(Modifier.height(10.dp))
            Rule()
            Spacer(Modifier.height(6.dp))

            // ── Footer ────────────────────────────────────────────────────────
            if (state.salesmanNameAr.isNotBlank()) {
                InfoLine(stringResource(Res.string.statement_salesman), state.salesmanNameAr)
            }
            InfoLine(
                stringResource(Res.string.statement_printed_at),
                state.printedAt.fullDateTime(),
                numeric = true,
            )

            Spacer(Modifier.height(14.dp))
            // Signature strip: this paper is often the thing a shopkeeper signs
            // to acknowledge the balance before the rep leaves.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SignatureSlot(stringResource(Res.string.statement_sign_customer), Modifier.weight(1f))
                SignatureSlot(stringResource(Res.string.statement_sign_salesman), Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            CenterLine(stringResource(Res.string.print_footer_thanks), size = 10)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatementPaperRow(row: StatementRow) {
    val label = when (row.docType) {
        "SALE" -> stringResource(Res.string.print_voucher_type_sale)
        "RETURN" -> stringResource(Res.string.print_voucher_type_return)
        "REQUEST" -> stringResource(Res.string.print_voucher_type_request)
        "PAYMENT" -> when (row.method) {
            "CASH" -> stringResource(Res.string.method_cash_label)
            "CHEQUE" -> stringResource(Res.string.method_cheque_label)
            "TRANSFER" -> stringResource(Res.string.method_transfer_label)
            else -> stringResource(Res.string.statement_payment)
        }
        else -> row.docType
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyCell(row.createdAt.dayMonth(), weight = 1.0f, numeric = true)
        Column(modifier = Modifier.weight(1.5f)) {
            Text(label, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.number, color = Ink, fontSize = 8.sp, style = LtrNum, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BodyCell(if (row.debit > 0) row.debit.jod() else "-", weight = 1.1f, numeric = true, end = true)
        BodyCell(if (row.credit > 0) row.credit.jod() else "-", weight = 1.1f, numeric = true, end = true)
        BodyCell(row.balance.jod(), weight = 1.2f, numeric = true, end = true, bold = true)
    }
}

// ── Small parts ───────────────────────────────────────────────────────────────

@Composable
private fun CenterLine(text: String, size: Int, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = Ink,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun InfoLine(label: String, value: String, numeric: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Ink, fontSize = 10.sp)
        Text(
            value,
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            style = if (numeric) LtrNum else TextStyle.Default,
        )
    }
}

@Composable
private fun TotalLine(label: String, value: String, bold: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Ink, fontSize = 11.sp)
        Text(
            value,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = LtrNum,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeadCell(
    text: String,
    weight: Float,
    end: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Ink,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
        maxLines = 1,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    numeric: Boolean = false,
    end: Boolean = false,
    bold: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Ink,
        fontSize = 9.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
        style = if (numeric) LtrNum else TextStyle.Default,
        maxLines = 1,
    )
}

@Composable
private fun SignatureSlot(label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
        Spacer(Modifier.height(3.dp))
        Text(label, color = Ink, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Rule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
}

@Composable
private fun ThinRule() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ink.copy(alpha = 0.35f)))
}

/** The torn edge that makes the block read as a till roll rather than a card. */
@Composable
private fun PaperTear(flipped: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val toothWidth = 10f
        val teeth = (size.width / toothWidth).toInt() + 1
        val path = Path()
        if (flipped) {
            path.moveTo(0f, 0f)
            for (i in 0 until teeth) {
                val x = i * toothWidth
                path.lineTo(x + toothWidth / 2, size.height)
                path.lineTo(x + toothWidth, 0f)
            }
            path.lineTo(size.width, 0f)
        } else {
            path.moveTo(0f, size.height)
            for (i in 0 until teeth) {
                val x = i * toothWidth
                path.lineTo(x + toothWidth / 2, 0f)
                path.lineTo(x + toothWidth, size.height)
            }
            path.lineTo(size.width, size.height)
        }
        path.close()
        drawRect(color = TearGray, topLeft = Offset.Zero, size = Size(size.width, size.height))
        drawPath(path, color = PaperBg)
    }
}

@Composable
private fun printerStatusLabel(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}

@Composable
private fun ActionChip(label: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) Blue else Color.White,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (filled) Color.White else DarkBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
