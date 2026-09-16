package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.feature.print.PrinterConnectDialog
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.PaymentType
import com.jehadalomour.flowvan.core.model.VoucherTemplate
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.feature.print.VoucherPrintEvent
import com.jehadalomour.flowvan.feature.print.VoucherPrintState
import com.jehadalomour.flowvan.feature.print.VoucherPrintViewModel
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Design tokens ────────────────────────────────────────────────────────────

internal val RcBg        = Color.White
private val ScreenBg    = Color(0xFFD1D5DB)
private val Dark        = Color(0xFF0F1923)
private val Blue        = Color(0xFF185FA5)
private val DarkBlue    = Color(0xFF1A2A3A)
private val Green       = Color(0xFF1D9E75)
private val SubText     = Color(0xFF637181)
private val DivItem     = Color(0xFFE5E7EB)
private val TearGray    = Color(0xFFD1D5DB)

// ── Receipt ink ───────────────────────────────────────────────────────────────
// One ink, because the head has one: it burns a dot or it leaves the paper white. Everything
// this receipt used to carry its hierarchy in — a #637181 label, a #AAB5C6 hairline, the blue
// on the grand total — had no way to reach the roll, so the printer approximated each tone by
// scattering dots and the voucher came back as speckle with the solid rules still running
// through it. So the paper no longer resolves ink through a palette at all: label-vs-value is
// weight, section-vs-row is size, and blocks are fenced by rules.
//
// VoucherTemplate.monochrome therefore selects nothing here; the coloured voucher lives on in
// VoucherA4Document, which is read on a phone or printed by a real printer. The roles that
// used to be muted/faint/accent/negative/warn are all this one value now — do not reintroduce
// a hue or a grey on this paper to get an emphasis back, reach for weight or size instead.
private val RcInk = ThermalInk.Ink

// Force Western (Latin) digits + LTR for every number on the voucher, even under an Arabic
// locale where ASCII digits would otherwise shape as Arabic-Indic (٠١٢…).
private val LtrNum = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))
// Same Latin-digit shaping but keeps the paragraph direction (for Arabic text that embeds a number).
private val RtlNum = TextStyle(localeList = LocaleList("en-US"))

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun VoucherPrintScreen(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: VoucherPrintViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsState()
    // The A4 document is rendered off-screen; this layer captures it for "Share as PDF".
    val graphicsLayer = rememberGraphicsLayer()
    // The thermal receipt is captured off-screen at head resolution (see ThermalCapture below);
    // this layer holds that bitmap. The on-screen preview is the same paper, but is not the source.
    val thermalLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    // Print-only line compacting. Asked ONCE per opening of this screen, before the receipt
    // is printed or shared, and only when there is actually something to merge — so a normal
    // single-unit voucher never sees a dialog. null = not answered yet.
    // This screen is the single entry point for both "just saved" and "reprint from reports",
    // so asking here covers both without duplicating the decision.
    var compact by remember(state.invoiceId) { mutableStateOf<CompactMode?>(null) }
    // Two levels: same-item unit merges, and (a superset) same-priced alternative merges.
    val unitMergeable = remember(state.lines) { compactableCount(state.lines, mergeAlternatives = false) }
    // The alternatives level folds DIFFERENT items, so it needs the ERP's groups: without
    // them the count is the unit count and the dialog never offers the second button.
    val altMergeable = remember(state.lines, state.altGroupBySku) {
        compactableCount(state.lines, mergeAlternatives = true, altGroups = state.altGroupBySku)
    }
    val shownLines = remember(state.lines, state.altGroupBySku, compact) {
        when (compact) {
            CompactMode.ALTERNATIVES ->
                compactLines(state.lines, mergeAlternatives = true, altGroups = state.altGroupBySku)
            CompactMode.UNITS -> compactLines(state.lines, mergeAlternatives = false)
            else -> state.lines
        }
    }

    // Capture the receipt and send it to the ViewModel as PNG bytes.
    // Bitmap capture is the one piece that must live in the UI; all logic stays in the VM.
    suspend fun captureAndPrint() {
        // Thermal print uses the off-screen receipt capture, NOT the A4 document.
        val bitmap = thermalLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(VoucherPrintEvent.Print(png))
    }

    // Auto-print once a connection is established from the dialog.
    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (state.pendingPrint && state.printerState is PrinterState.Connected) {
            captureAndPrint()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg),
    ) {
        // Top bar
        Surface(color = DarkBlue, shadowElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_back), contentDescription = null, tint = Color.White)
                }
                Text(stringResource(Res.string.print_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
            return@Column
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
            ActionButton(
                label = stringResource(Res.string.printer_thermal_print),
                filled = true,
                onClick = {
                    if (state.printerState is PrinterState.Connected) {
                        scope.launch { captureAndPrint() }
                    } else {
                        viewModel.onEvent(VoucherPrintEvent.RequestConnectThenPrint)
                    }
                },
            )
            Spacer(Modifier.size(10.dp))
            ActionButton(
                label = stringResource(Res.string.print_action_share_pdf),
                filled = false,
                onClick = {
                    scope.launch {
                        // Share captures the off-screen A4 document and shares it as an A4 PDF.
                        val bmp = graphicsLayer.toImageBitmap()
                        pdfHelper.shareAsPdf(bmp, state.number, a4 = true)
                    }
                },
            )
        }

        // Printer status + last action feedback
        val statusMessage = when {
            state.isPrinting -> stringResource(Res.string.printer_printing)
            state.printMessageAr != null -> state.printMessageAr!!
            else -> printerStatusLabel(state.printerState)
        }
        Text(
            text = statusMessage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = if (state.printerState is PrinterState.Connected) Green else SubText,
        )

        if (!state.isLoading && altMergeable > 0 && compact == null) {
            CompactLinesDialog(
                unitCount = unitMergeable,
                altCount = altMergeable,
                onMergeUnits = { compact = CompactMode.UNITS },
                onMergeAlternatives = { compact = CompactMode.ALTERNATIVES },
                onKeepAll = { compact = CompactMode.NONE },
            )
        }

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = { viewModel.onEvent(VoucherPrintEvent.ConnectTypeSelected(it)) },
                connectLanguage = state.connectLanguage,
                onLanguageSelected = { viewModel.onEvent(VoucherPrintEvent.PrinterLanguageSelected(it)) },
                onAddressChanged = { viewModel.onEvent(VoucherPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(VoucherPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(VoucherPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(VoucherPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(VoucherPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(VoucherPrintEvent.DismissConnectDialog) },
            )
        }

        // Scrollable receipt preview — what the rep looks at, drawn at the screen's density. It
        // is no longer the print source: the bitmap now comes from the off-screen ThermalCapture
        // below, so the printed resolution stops depending on which phone or terminal is running
        // the app. Sharing still uses the off-screen A4 further down.
        // Fixed-width paper — see requiredWidth below — so a screen narrower
        // than it pans rather than clipping a column off the edge.
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .requiredWidth(320.dp)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(RcBg, RoundedCornerShape(4.dp)),
            ) {
                Column {
                    // Decorative torn edges stay in the preview only. They are grey strips, which
                    // is the one thing a 1-bit head cannot print, and the capture below takes the
                    // body alone.
                    ReceiptTear()
                    ReceiptBody(state, shownLines)
                    ReceiptTear(flipped = true)
                }
            }

            // The print source: the same paper, rendered off-screen at head resolution and
            // recorded into the layer thermal printing reads. 320.dp is the width this
            // receipt's layout was designed against — keep it, or every column reflows.
            ThermalCapture(layer = thermalLayer, paperDp = 320.dp) {
                ReceiptBody(state, shownLines)
            }

            Spacer(Modifier.height(32.dp))
        }

        // Off-screen A4 document — captured for "Share as PDF" only, never painted to screen.
        //
        // The outer layout measures the A4 at its natural size (fixed width, UNBOUNDED height)
        // but then reports zero size to the parent, so the document occupies no space on screen
        // while still being drawn at full size into graphicsLayer. A plain size(0) wrapper would
        // instead clamp the child's height to 0, and toImageBitmap() would crash on a 0×0 layer.
        Box(
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                layout(0, 0) { placeable.place(0, 0) }
            },
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(794.dp)
                    .background(Color.White)
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                    },
            ) {
                VoucherA4Document(state)
            }
        }
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) DarkBlue else RcBg,
        shadowElevation = if (filled) 2.dp else 0.dp,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD1D5DB)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (filled) Color.White else Dark,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

// ── Tear edge ─────────────────────────────────────────────────────────────────

@Composable
internal fun ReceiptTear(flipped: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
        val toothW = 10.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            val path = Path().apply {
                val top = if (flipped) size.height else 0f
                val bot = if (flipped) 0f else size.height
                moveTo(x, top)
                lineTo(x + toothW / 2f, top)
                lineTo(x + toothW / 2f + size.height, bot)
                lineTo(x + size.height, bot)
                close()
            }
            drawPath(path, TearGray)
            x += toothW
        }
    }
}

// ── Receipt body ──────────────────────────────────────────────────────────────

@Composable
internal fun ReceiptBody(
    state: VoucherPrintState,
    /** The lines to PRINT — either state.lines verbatim, or the compacted view when the
     *  rep chose to merge same-item/same-factor rows. Never the saved data. */
    shownLines: List<InvoiceLine> = state.lines,
) {
    val t = state.template
    // The printed voucher is Arabic-first and must always lay out right-to-left, no matter the
    // device/app locale — labels sit on the right, values on the left. Numbers stay LTR via LtrNum.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        val isArabic = Locale.current.language.startsWith("ar")
        val paymentType = PaymentType.fromPaymentMethod(state.paymentMethod)
        val paymentValue = if (isArabic) paymentType.labelAr else paymentType.labelEn

        // Company header — always the Arabic company name (fall back to the English one only
        // if no Arabic name was provided). The printed voucher never shows the English name.
        val companyName = state.companyNameAr.ifBlank { state.companyNameEn }
        val taxLabel = if (isArabic) "الرقم الضريبي" else "Tax No."

        Column(modifier = Modifier.background(RcBg).padding(14.dp)) {

            // Company header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Logo at the top — the company's own logo (cached from /company-info) when
                // available, else the bundled default (tinted to the one ink).
                //
                // 110.dp, not the 250.dp this used to be: an uploaded logo is a continuous-tone
                // photograph, and at 250.dp on 320.dp of paper the head was being handed a
                // greyscale image across 78% of the page, which it can only dither. Small keeps
                // the dithering down to a mark instead of a field of noise. The tint on the
                // bundled vector does NOT make it 1-bit either — it multiplies colour and leaves
                // the alpha channel — so the cap applies to both.
                val logoBitmap = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(110.dp).padding(bottom = 8.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(RcInk),
                        modifier = Modifier.size(110.dp).padding(bottom = 8.dp),
                    )
                }
                if (companyName.isNotBlank()) {
                    Text(
                        text = companyName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = ThermalInk.FS_COMPANY.sp,
                        color = RcInk,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                    )
                }
                val subLine = buildString {
                    if (state.branch.isNotBlank()) append(state.branch)
                    if (state.companyTaxNumber.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("$taxLabel: ${state.companyTaxNumber}")
                    }
                }
                if (subLine.isNotBlank()) {
                    Text(
                        text = subLine,
                        fontSize = ThermalInk.FS_SUB.sp,
                        fontWeight = FontWeight.Bold,
                        color = RcInk,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        style = RtlNum,
                    )
                }
            }

            SepSolid()

            // Voucher type tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypeTag(label = typeLabel(state.type))
            }

            SepDash()

            // Invoice meta
            KvRow(stringResource(Res.string.voucher_detail_number), "#${state.number}")
            KvRow(stringResource(Res.string.voucher_detail_date), state.createdAt.toReceiptDateStr())
            KvRow(stringResource(Res.string.print_salesman), state.salesmanNameAr)

            SepDash()

            // Customer
            KvRow(stringResource(Res.string.print_customer), state.customerNameAr)
            KvRow(stringResource(Res.string.print_customer_code), state.customerCode)
            state.customerTaxNumber?.let { KvRow(stringResource(Res.string.print_customer_tax_number), it) }

            // Payment type — header (outlined box)
            if (t.showPaymentType && t.paymentTypeInHeader) {
                SepDash()
                PaymentTypeHeaderRow(
                    label = stringResource(Res.string.print_payment_method_label),
                    value = paymentValue,
                )
            }

            SepSolid()

            // Column headers
            ItemColHeader(showLineDiscount = state.canPrintLineDiscount, showTax = !state.isTaxExempt)

            // Items (purchased, then gift lines — each a normal item at 100% discount)
            shownLines.forEach { line ->
                ReceiptItemRow(line, t.amountDecimals, state.canPrintLineDiscount, showTax = !state.isTaxExempt)
            }
            state.freeLines.forEach { line ->
                ReceiptItemRow(line, t.amountDecimals, state.canPrintLineDiscount, isGift = true, showTax = !state.isTaxExempt)
            }

            SepDash()

            // Totals. Gift lines carry a real price fully discounted, so their gross adds to
            // both the subtotal and the line discount — the columns foot and the net is 0.
            val freeGross = state.freeLines.sumOf { it.qty * it.unitPrice }
            TotRow(stringResource(Res.string.voucher_detail_subtotal), money(state.subtotal + freeGross, t))
            // Discount breakdown. When offers were applied we ITEMIZE each offer (name + value)
            // then a total-offer-discount row, and drop the generic aggregate rows — for a SALE
            // the offers ARE the discount, so the generic rows would just duplicate the total.
            // No offers (or a RETURN/ORDER) → fall back to the generic line/total discount rows.
            // A discount used to be red and the tax amber. On paper the sign carries it: the
            // leading "-" and "+" are already in the value string, and they print.
            if (state.appliedOffers.isNotEmpty()) {
                state.appliedOffers.forEach { offer ->
                    TotRow(offer.name, "- ${money(offer.discountAmount, t)}")
                }
                val offersTotal = state.appliedOffers.sumOf { it.discountAmount }
                TotRow(stringResource(Res.string.print_offers_total), "- ${money(offersTotal, t)}")
            } else {
                val lineDiscount = shownLines.sumOf { it.qty * it.unitPrice * it.discountPct } + freeGross
                if (lineDiscount > 0.0005) {
                    TotRow(stringResource(Res.string.print_line_discount), "- ${money(lineDiscount, t)}")
                }
                if (state.discountAmount > 0.0005) {
                    TotRow(stringResource(Res.string.print_total_discount), "- ${money(state.discountAmount, t)}")
                }
            }
            if (state.taxAmount > 0.0) {
                TotRow(taxTotalLabel(shownLines), "+ ${money(state.taxAmount, t)}")
            }
            TotRow(
                stringResource(Res.string.print_item_count),
                (shownLines.sumOf { it.qty } + state.freeLines.sumOf { it.qty }).let {
                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                }
            )
            if (t.showPaymentType && t.paymentTypeInFooter) {
                PaymentTypeFooterRow(
                    label = stringResource(Res.string.print_payment_method_label),
                    value = paymentValue,
                )
            }

            // Grand total
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(ThermalInk.RuleThickness, RcInk),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.voucher_detail_total),
                    // Wraps rather than squeezing the number: the total is what the paper
                    // exists to state, and it must never be the part that gets clipped.
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 10.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = ThermalInk.FS_TOTAL.sp,
                    color = RcInk,
                )
                Text(
                    text = money(state.total, t),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontWeight = FontWeight.ExtraBold,
                    // Already 24 — the one place the old design was big enough. Left as it is
                    // because it is the biggest number on the paper and it fits at 320.dp.
                    fontSize = 24.sp,
                    color = RcInk,
                    style = LtrNum,
                )
            }

            // Notes
            state.notes?.let { notes ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.print_notes_prefix, notes),
                    fontSize = ThermalInk.FS_MIN.sp,
                    fontWeight = FontWeight.Bold,
                    color = RcInk,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Signature — recipient only (stamp box removed)
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                SignatureBox(stringResource(Res.string.print_signature_recipient))
            }

            // Tax QR (JoFotara/ISTD) — only when a payload exists; omitted entirely otherwise.
            state.qrData?.let {
                Spacer(Modifier.height(10.dp))
                Image(
                    painter = rememberQrCodePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t.qrCaption,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = ThermalInk.FS_MIN.sp,
                    fontWeight = FontWeight.Bold,
                    color = RcInk,
                    lineHeight = 20.sp,
                )
            }

            // Star separator
            Spacer(Modifier.height(8.dp))
            Text(
                text = "* * * * * * * *",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = ThermalInk.FS_ROW.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RcInk,
                letterSpacing = 3.sp,
            )

            // Footer
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.print_footer_thanks),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = ThermalInk.FS_ROW.sp,
                    color = RcInk,
                )
                Spacer(Modifier.height(2.dp))
                Text("Powered by 7Software", fontSize = ThermalInk.FS_MIN.sp, fontWeight = FontWeight.Bold, color = RcInk, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Item row ──────────────────────────────────────────────────────────────────

/**
 * Column weights for the item grid, in the order the cells are built. Used by BOTH the heading
 * row and the data rows, so the two stay in register.
 *
 * Equal weights were fine at 11sp. At the 16sp floor they are not: the columns share 292.dp of
 * paper (320 less the body's padding), and an even six-way split gives each about 48.dp — which
 * holds neither "1234.500" nor the heading "الإجمالي". So each column gets roughly what its
 * WIDEST occupant needs, heading or cell: the money columns are wide because of their numbers,
 * the tax column because of its heading, qty and unit because six Arabic headings all want
 * about the same room.
 *
 * Worth knowing: six columns only happens when a voucher shows per-line tax AND the salesman
 * may print per-line discounts. That configuration is at the edge of what 80mm can carry at the
 * 16sp floor. If it starts clipping, drop a column — that is a product decision. Do not answer
 * it by taking the grid back under 16sp: that is what made these receipts print as speckle.
 */
private const val W_QTY = 0.95f
private const val W_UNIT = 0.95f
private const val W_TAX = 1.0f
private const val W_PRICE = 1.1f
private const val W_DISCOUNT = 1.0f
private const val W_TOTAL = 1.25f

private fun itemColWeights(showLineDiscount: Boolean, showTax: Boolean): List<Float> = buildList {
    add(W_QTY); add(W_UNIT)
    if (showTax) add(W_TAX)
    add(W_PRICE)
    if (showLineDiscount) add(W_DISCOUNT)
    add(W_TOTAL)
}

@Composable
private fun ItemColHeader(showLineDiscount: Boolean = false, showTax: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        val weights = itemColWeights(showLineDiscount, showTax)
        buildList {
            add(stringResource(Res.string.print_col_qty))
            add(stringResource(Res.string.print_col_unit))
            // Tax-exempt voucher: no per-line tax column at all.
            if (showTax) add(stringResource(Res.string.print_col_tax))
            add(stringResource(Res.string.print_col_price))
            // Sits BEFORE the total, so the eye reads price -> discount -> total.
            if (showLineDiscount) add("خصم")
            add(stringResource(Res.string.print_col_total))
        }.forEachIndexed { idx, label ->
            Text(
                text = label,
                modifier = Modifier.weight(weights[idx]),
                textAlign = TextAlign.Center,
                fontSize = ThermalInk.FS_HEAD.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RcInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    // The heavy rule under the headings — 2.dp solid, which is the thinnest mark a head prints
    // as a line rather than as a broken row of dots. Drawn down the middle of the Canvas, not
    // along y=0: a stroke centred on the top edge loses its upper half to the clip, and half of
    // 2.dp is back to the hairline this is replacing.
    Canvas(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness)) {
        val y = size.height / 2f
        drawLine(RcInk, Offset(0f, y), Offset(size.width, y), strokeWidth = ThermalInk.RuleThickness.toPx())
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun ReceiptItemRow(
    line: InvoiceLine,
    amountDecimals: Int,
    /** Granted per salesman (canPrintLineDiscount) — see the column comment below. */
    showLineDiscount: Boolean = false,
    /** A line the customer is not paying for; labelled so nobody reads it as sold. */
    isGift: Boolean = false,
    /** False on a tax-exempt voucher — the per-line tax column is dropped. */
    showTax: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        // Product name — Arabic, always reads right-to-left (right-aligned) on the printout.
        Text(
            // A gift line is priced and then fully discounted, so without the label
            // it prints as a normal item the customer appears to have bought.
            text = if (isGift) "${line.nameAr} (gift . هدية)" else line.nameAr,
            fontWeight = FontWeight.ExtraBold,
            fontSize = ThermalInk.FS_ROW.sp,
            color = RcInk,
            textAlign = TextAlign.Right,
            style = TextStyle(textDirection = TextDirection.Rtl),
            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
        )
        // 5-column data grid. The per-line discount column is deliberately NOT printed.
        //
        // The printed line total is the GROSS (qty × unitPrice) — it deliberately excludes
        // both the discount and the tax. Those are each stated ONCE, in the footer, which
        // opens with a Subtotal of Σ(qty × unitPrice) and then subtracts the discount and
        // adds the tax. Printing the net here instead (lineTotal, which nets the discount
        // and adds the line's tax) made the column foot to nothing and showed the customer
        // the same discount twice — once silently inside every line, once in the footer.
        // The net per line is still shown on-screen in the voucher detail.
        // The optional discount column is INFORMATION beside the gross, not a
        // substitute for it: the total column and the footer are unchanged whether it
        // is shown or not, so the receipt foots either way.
        Row(modifier = Modifier.fillMaxWidth()) {
            val qty = formatQty(line.qty)
            val unit = line.unit.ifBlank { "—" }
            val taxPct = line.taxPctLabel()
            // A gift is 100% discounted, so the customer pays nothing for it —
            // print 0, not the notional price they might think they are charged.
            // Its value is still inside the subtotal and the offer discount below,
            // which is what makes the subtotal → discount → total chain foot.
            val price = formatAmount(if (isGift) 0.0 else line.unitPrice, amountDecimals)
            val gross = line.qty * line.unitPrice
            val discount = formatAmount(if (isGift) gross else gross * line.discountPct, amountDecimals)
            val total = formatAmount(if (isGift) 0.0 else gross, amountDecimals)

            buildList {
                add(qty); add(unit)
                if (showTax) add(taxPct)
                add(price)
                if (showLineDiscount) add(discount)
                add(total)
            }.let { cells ->
              val weights = itemColWeights(showLineDiscount, showTax)
              val lastIdx = cells.lastIndex
              cells.forEachIndexed { idx, cell ->
                Text(
                    text = cell,
                    modifier = Modifier.weight(weights[idx]),
                    textAlign = TextAlign.Center,
                    // The grid is the one place held at the floor: six columns have to share
                    // 292.dp of paper, and FS_MIN is where Arabic and the decimals still resolve.
                    fontSize = ThermalInk.FS_MIN.sp,
                    // Emphasise the LAST cell (the total), not index 4 — the optional
                    // discount column shifts the total from 4 to 5, and a hardcoded
                    // index would emphasise the discount instead.
                    // Column 1 is the unit, emphasised too: with colour units it is what
                    // tells the customer WHICH variant this line is — the sku repeats.
                    // The rest is Bold and nothing is lighter: SemiBold stems are thinner
                    // than a dot and came off the roll as broken letters.
                    fontWeight = if (idx == lastIdx || idx == 1) FontWeight.ExtraBold else FontWeight.Bold,
                    color = RcInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LtrNum,
                )
              }
            }
        }
        // SKU
        Text(
            text = line.sku,
            fontSize = ThermalInk.FS_MIN.sp,
            fontWeight = FontWeight.Bold,
            color = RcInk,
        )
    }
    // Between items. Still dashed, because that is what separates one item from the section
    // rules around the table — but in pure black at 2.dp with a coarse 6/4 pattern. The old
    // 1.dp grey dash at 4/4 was the least printable mark on the page: every dot of it was a
    // decision the head had to guess at.
    Canvas(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness)) {
        val dashLen = 6.dp.toPx(); val gap = 4.dp.toPx()
        val y = size.height / 2f
        var x = 0f
        while (x < size.width) {
            drawLine(
                RcInk,
                Offset(x, y),
                Offset((x + dashLen).coerceAtMost(size.width), y),
                strokeWidth = ThermalInk.RuleThickness.toPx(),
            )
            x += dashLen + gap
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

/** The heavy divider: a solid black rule, edge to edge, fencing the big blocks of the paper. */
@Composable
private fun SepSolid() {
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness)) {
        val y = size.height / 2f
        drawLine(RcInk, Offset(0f, y), Offset(size.width, y), strokeWidth = ThermalInk.RuleThickness.toPx())
    }
    Spacer(Modifier.height(8.dp))
}

/**
 * The light divider between the meta blocks. It stays dashed — solid everywhere would flatten
 * the block structure that SepSolid is carrying — but the lightness now comes from the gaps,
 * not from grey: pure black, 2.dp, a coarse 6-on/4-off pattern the head can actually resolve.
 */
@Composable
private fun SepDash() {
    Spacer(Modifier.height(7.dp))
    Canvas(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness)) {
        val dash = 6.dp.toPx(); val gap = 4.dp.toPx()
        val y = size.height / 2f
        var x = 0f
        while (x < size.width) {
            drawLine(
                RcInk,
                Offset(x, y),
                Offset((x + dash).coerceAtMost(size.width), y),
                strokeWidth = ThermalInk.RuleThickness.toPx(),
            )
            x += dash + gap
        }
    }
    Spacer(Modifier.height(7.dp))
}

// Label-vs-value used to be grey-vs-black. It is Bold-vs-ExtraBold now, and both are black.
// The label takes weight(fill = false) so that at 18sp it wraps instead of squeezing the value:
// a wrapped Arabic label still reads, a clipped number does not.
@Composable
private fun KvRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.Bold,
            color = RcInk,
        )
        Text(
            text = value,
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RcInk,
            style = LtrNum,
        )
    }
}

@Composable
private fun TotRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Offer names arrive here and can be long — this is the row most likely to need the wrap.
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.Bold,
            color = RcInk,
        )
        Text(
            text = value,
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RcInk,
            style = LtrNum,
        )
    }
}

/** The document's title tag: square black box, black text. A rounded corner is a grey arc. */
@Composable
private fun TypeTag(label: String) {
    Box(
        modifier = Modifier
            .border(ThermalInk.RuleThickness, RcInk)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = ThermalInk.FS_TITLE.sp, color = RcInk)
    }
}

/** Payment type in the header info block: label right, value in a square outlined box (no fill). */
@Composable
private fun PaymentTypeHeaderRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.Bold,
            color = RcInk,
        )
        Box(
            modifier = Modifier
                .border(ThermalInk.RuleThickness, RcInk)
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = ThermalInk.FS_ROW.sp, color = RcInk)
        }
    }
}

/** Payment type in the totals block: plain bold, no box, to keep the totals calm. */
@Composable
private fun PaymentTypeFooterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.Bold,
            color = RcInk,
        )
        Text(value, fontSize = ThermalInk.FS_ROW.sp, fontWeight = FontWeight.ExtraBold, color = RcInk)
    }
}

@Composable
private fun SignatureBox(label: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = ThermalInk.FS_ROW.sp, fontWeight = FontWeight.Bold, color = RcInk)
        Spacer(Modifier.height(16.dp))
        // The line the customer signs on: solid black, so there is something on the paper to
        // sign on. The old 1.5.dp grey hairline printed as a dotted smudge.
        Canvas(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness)) {
            val y = size.height / 2f
            drawLine(RcInk, Offset(0f, y), Offset(size.width, y), strokeWidth = ThermalInk.RuleThickness.toPx())
        }
    }
}

// ── Pure helpers ──────────────────────────────────────────────────────────────

@Composable
private fun printerStatusLabel(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}

@Composable
private fun typeLabel(type: String) = when (type) {
    "SALE"    -> stringResource(Res.string.print_voucher_type_sale)
    "RETURN"  -> stringResource(Res.string.print_voucher_type_return)
    "REQUEST" -> stringResource(Res.string.print_voucher_type_request)
    else      -> type
}

/** Tax column shows the rate as a percentage (e.g. "16%"), never the tax type. */
private fun InvoiceLine.taxPctLabel(): String =
    if (taxRate > 0.0) "${(taxRate * 100).roundToInt()}%" else "—"

/**
 * Footer tax label: "إجمالي الضريبة (16%)" when every taxed line shares one rate; the bare
 * "إجمالي الضريبة" when rates are mixed. Digits are always Latin, in every locale.
 */
@Composable
private fun taxTotalLabel(lines: List<InvoiceLine>): String {
    val base = stringResource(Res.string.print_tax_total)
    val rates = lines
        .filter { it.taxType != "EXEMPT" && it.taxRate > 0.0 }
        .map { it.taxRate }
        .distinct()
    if (rates.size != 1) return base
    val pct = (rates.first() * 100).roundToInt()
    return "$base ($pct%)"
}

internal fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else formatAmount(qty, 2)

/**
 * Bare number at the given decimal precision, built from Latin digits explicitly. We do NOT use
 * String.format/"%f": it follows the default locale, which the app forces to Arabic, so it would
 * emit Arabic-Indic numerals (٠١٢) and an Arabic decimal separator. Long.toString() is always Latin.
 */
private fun formatAmount(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val whole = scaled / factor
    val frac = scaled % factor
    val sb = StringBuilder()
    if (value < 0) sb.append('-')
    sb.append(whole.toString())
    if (decimals > 0) sb.append('.').append(frac.toString().padStart(decimals, '0'))
    return sb.toString()
}

/** Number + currency symbol, e.g. "123.450 د.أ". */
private fun money(value: Double, t: VoucherTemplate): String =
    "${formatAmount(value, t.amountDecimals)} ${t.currency}"

private fun Long.toReceiptDateStr(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/" +
        "${dt.year}  " +
        "${dt.hour.toString().padStart(2, '0')}:" +
        dt.minute.toString().padStart(2, '0')
}
