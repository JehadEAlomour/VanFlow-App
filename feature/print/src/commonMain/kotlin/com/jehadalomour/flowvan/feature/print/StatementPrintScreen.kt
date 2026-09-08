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

// ── Type scale ───────────────────────────────────────────────────────────────
// One place, because this is the thing that gets tuned against real paper.
//
// What matters on a thermal roll is the ratio of text size to CAPTURE WIDTH: the
// printer scales the bitmap to the head's 576 dots regardless, so enlarging both
// together changes nothing on paper. The width below went up 20% for a sharper
// raster and the type went up ~30%, which is the part that actually lands bigger
// in the customer's hand.

/** Capture width. Wider = more source pixels for the printer to scale down. */
private val PaperWidth = 384.dp

private const val FS_COMPANY = 26      // matches the voucher receipt exactly
private const val FS_COMPANY_SUB = 15  // latin name, tax number
private const val FS_TITLE = 20        // "كشف الحساب" in its inverted bar
private const val FS_INFO = 15         // customer / period / footer lines
private const val FS_TOTAL_LABEL = 16
private const val FS_TOTAL_VALUE = 18
private const val FS_HEAD = 14         // table column headings
private const val FS_ROW = 14          // table body
private const val FS_ROW_SUB = 12      // document number, on its own line
private const val FS_CLOSING_LABEL = 17
private const val FS_CLOSING_VALUE = 26 // the number the page exists to state
private const val FS_SIGN = 13
private const val FS_FOOTER = 14

/**
 * Logo box, sized to the voucher receipt's 250dp AT ITS 320dp paper — scaled to
 * this page's wider capture so the mark lands the same size in the hand. Both
 * bitmaps are squeezed to the head's 576 dots, so only the ratio to paper width
 * survives; copying 250dp verbatim onto wider paper would print it smaller.
 */
private val LogoSize = 300.dp

/** Everything on the paper is bold. A thermal head lays down a thin, slightly
 *  fibrous line, and regular weight at this size greys out under shop lighting —
 *  the receipt is read once, quickly, by someone not looking for it. */
private val PaperWeight = FontWeight.Bold

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
 * Same two exits as every other receipt: the thermal roll for the person
 * standing in front of the rep, and share-as-PDF for the one who is not there.
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
                    scope.launch { pdfHelper.shareAsPdf(graphicsLayer.toImageBitmap(), docName, a4 = true) }
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
                    .widthIn(max = PaperWidth)
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
                    // NOT tinted. The company's logo arrives as whatever they
                    // uploaded — this client's is a JPEG, which has no
                    // transparency, so tinting it would paint the whole
                    // rectangle and print a solid black box where the mark
                    // should be. Only the bundled vector below is tinted,
                    // because it is a monochrome shape drawn for exactly that.
                    Image(
                        bitmap = logo,
                        contentDescription = null,
                        modifier = Modifier.size(LogoSize),
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.size(LogoSize),
                        colorFilter = ColorFilter.tint(Ink),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            CenterLine(state.companyNameAr, size = FS_COMPANY, bold = true)
            if (state.companyNameEn.isNotBlank()) CenterLine(state.companyNameEn, size = FS_COMPANY_SUB)
            if (state.companyTaxNumber.isNotBlank()) {
                CenterLine(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    size = FS_COMPANY_SUB,
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
                    fontSize = FS_TITLE.sp,
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
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                // Weights match line 1 of a row. The document number is no
                // longer a column — it has its own line underneath — so the
                // money columns get the width the larger type needs.
                HeadCell(stringResource(Res.string.statement_col_date), weight = 0.9f)
                HeadCell(stringResource(Res.string.statement_col_doc), weight = 1.1f)
                HeadCell(stringResource(Res.string.statement_col_debit), weight = 1.25f)
                HeadCell(stringResource(Res.string.statement_col_credit), weight = 1.25f)
                HeadCell(stringResource(Res.string.statement_col_balance), weight = 1.5f)
            }
            Rule()

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                CenterLine(stringResource(Res.string.statement_empty), size = FS_INFO)
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
                        fontSize = FS_CLOSING_LABEL.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.closingBalance.jod(),
                        color = PaperBg,
                        fontSize = FS_CLOSING_VALUE.sp,
                        fontWeight = FontWeight.Bold,
                        style = LtrNum,
                    )
                }
            }

            // A negative closing balance means the shop is in credit — rare, and
            // exactly the case someone will otherwise read as a debt.
            if (state.closingBalance < 0) {
                Spacer(Modifier.height(4.dp))
                CenterLine(stringResource(Res.string.statement_in_credit), size = FS_FOOTER, bold = true)
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
            CenterLine(stringResource(Res.string.print_footer_thanks), size = FS_FOOTER)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatementPaperRow(row: StatementRow) {
    // No order label: an order is not on a statement — see CustomerStatement.
    val label = when (row.docType) {
        "SALE" -> stringResource(Res.string.print_voucher_type_sale)
        "RETURN" -> stringResource(Res.string.print_voucher_type_return)
        "PAYMENT" -> when (row.method) {
            "CASH" -> stringResource(Res.string.method_cash_label)
            "CHEQUE" -> stringResource(Res.string.method_cheque_label)
            "TRANSFER" -> stringResource(Res.string.method_transfer_label)
            else -> stringResource(Res.string.statement_payment)
        }
        else -> row.docType
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
        // Line 1 — the money. Everything a reader scans down the page for.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BodyCell(row.createdAt.dayMonth(), weight = 0.9f, numeric = true)
            BodyCell(label, weight = 1.1f)
            BodyCell(if (row.debit > 0) row.debit.jod() else "-", weight = 1.25f, numeric = true)
            BodyCell(if (row.credit > 0) row.credit.jod() else "-", weight = 1.25f, numeric = true)
            BodyCell(row.balance.jod(), weight = 1.5f, numeric = true, bold = true)
        }
        // Line 2 — the document number, on its own line beneath the amounts and
        // running LEFT TO RIGHT. It used to sit stacked under the type in a
        // narrow column, where a long number ellipsised and the RTL paragraph
        // pushed it to the far edge; a reference someone has to quote back down
        // the phone is the last thing that should be clipped or mirrored.
        // TextAlign.Left is absolute, not start-relative, so it stays left under
        // the RTL layout the rest of the paper uses.
        Text(
            text = row.number,
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            fontSize = FS_ROW_SUB.sp,
            fontWeight = PaperWeight,
            style = LtrNum,
            textAlign = TextAlign.Left,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        fontWeight = if (bold) FontWeight.ExtraBold else PaperWeight,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun InfoLine(label: String, value: String, numeric: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Ink, fontSize = FS_INFO.sp, fontWeight = PaperWeight)
        Text(
            value,
            color = Ink,
            fontSize = FS_INFO.sp,
            fontWeight = FontWeight.ExtraBold,
            style = if (numeric) LtrNum else TextStyle.Default,
        )
    }
}

@Composable
private fun TotalLine(label: String, value: String, bold: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Ink, fontSize = FS_TOTAL_LABEL.sp, fontWeight = PaperWeight)
        Text(
            value,
            color = Ink,
            fontSize = FS_TOTAL_VALUE.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else PaperWeight,
            style = LtrNum,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeadCell(
    text: String,
    weight: Float,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Ink,
        fontSize = FS_HEAD.sp,
        fontWeight = FontWeight.ExtraBold,
        // Right, absolutely — see BodyCell.
        textAlign = TextAlign.Right,
        maxLines = 1,
    )
}

/**
 * A table cell.
 *
 * `TextAlign.Right`, never `End`. End is resolved against the paragraph's TEXT
 * direction, and the two halves of this table do not agree on one: a heading
 * inherits the paper's RTL, while the number under it carries [LtrNum] to keep
 * its digits Latin — which also flips it to LTR. So `End` meant the left edge
 * for the heading and the right edge for the figure, and every column came out
 * with its title on the opposite side from its contents. Absolute alignment
 * removes the disagreement: heading and figure cannot drift apart because
 * neither is asking which way the text runs.
 *
 * Right for every column, including the leading ones — right IS the start on
 * RTL paper, and money reads correctly when the digits share an edge.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    numeric: Boolean = false,
    bold: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Ink,
        fontSize = FS_ROW.sp,
        fontWeight = if (bold) FontWeight.ExtraBold else PaperWeight,
        textAlign = TextAlign.Right,
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
        Text(label, color = Ink, fontSize = FS_SIGN.sp, fontWeight = PaperWeight, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
