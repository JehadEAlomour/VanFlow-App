package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
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

private val RcBg        = Color.White
private val ScreenBg    = Color(0xFFD1D5DB)
private val Dark        = Color(0xFF0F1923)
private val Blue        = Color(0xFF185FA5)
private val DarkBlue    = Color(0xFF1A2A3A)
private val Green       = Color(0xFF1D9E75)
private val Red         = Color(0xFFD94040)
private val Amber       = Color(0xFFC97B1A)
private val SubText     = Color(0xFF637181)
private val DivLight    = Color(0xFFAAB5C6)
private val DivItem     = Color(0xFFE5E7EB)
private val TearGray    = Color(0xFFD1D5DB)

// ── Receipt palette ────────────────────────────────────────────────────────────
// Every ink/line/border in the receipt resolves through this palette so a single flag
// (VoucherTemplate.monochrome) swaps the whole voucher between colored and pure black/white.
// Monochrome conveys emphasis via weight/size/boxes only — no hue, ever (Jordan rollout).

private data class RcPalette(
    val ink: Color,        // primary text, solid rules, box borders
    val muted: Color,      // secondary labels (neutral grey, no hue)
    val faint: Color,      // faintest decorative rules / placeholders (neutral grey)
    val accent: Color,     // emphasised numbers (voucher #, grand total)
    val negative: Color,   // discount
    val warn: Color,       // tax
    val sale: Color,
    val ret: Color,
    val request: Color,
)

// Pure black on white: every ink role is #000000. Hierarchy comes from font weight/size and
// boxes/rules — never color (spec §3, a hard constraint for the Jordan market). Solid black also
// rasterises cleanly on 1-bit thermal heads, where any grey would dither into a stipple.
private val MonoPalette = RcPalette(
    ink = Color.Black,
    muted = Color.Black,
    faint = Color.Black,
    accent = Color.Black,
    negative = Color.Black,
    warn = Color.Black,
    sale = Color.Black,
    ret = Color.Black,
    request = Color.Black,
)

private val ColorPalette = RcPalette(
    ink = Dark,
    muted = SubText,
    faint = DivLight,
    accent = Blue,
    negative = Red,
    warn = Amber,
    sale = Green,
    ret = Red,
    request = Blue,
)

private val LocalRc = staticCompositionLocalOf { MonoPalette }

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
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    // Capture the on-screen receipt and send it to the ViewModel as PNG bytes.
    // Bitmap capture is the one piece that must live in the UI; all logic stays in the VM.
    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
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
                label = stringResource(Res.string.print_action_print),
                filled = false,
                onClick = {
                    scope.launch {
                        val bmp = graphicsLayer.toImageBitmap()
                        pdfHelper.printDocument(bmp, state.number)
                    }
                },
            )
            Spacer(Modifier.size(10.dp))
            ActionButton(
                label = stringResource(Res.string.print_action_share_pdf),
                filled = false,
                onClick = {
                    scope.launch {
                        val bmp = graphicsLayer.toImageBitmap()
                        pdfHelper.shareAsPdf(bmp, state.number)
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

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = { viewModel.onEvent(VoucherPrintEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { viewModel.onEvent(VoucherPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(VoucherPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(VoucherPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(VoucherPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(VoucherPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(VoucherPrintEvent.DismissConnectDialog) },
            )
        }

        // Scrollable receipt
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 320.dp)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(RcBg, RoundedCornerShape(4.dp))
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                Column {
                    ReceiptTear()
                    ReceiptBody(state)
                    ReceiptTear(flipped = true)
                }
            }
            Spacer(Modifier.height(32.dp))
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
private fun ReceiptTear(flipped: Boolean = false) {
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
private fun ReceiptBody(state: VoucherPrintState) {
    val t = state.template
    val palette = if (t.monochrome) MonoPalette else ColorPalette
    // The printed voucher is Arabic-first and must always lay out right-to-left, no matter the
    // device/app locale — labels sit on the right, values on the left. Numbers stay LTR via LtrNum.
    CompositionLocalProvider(
        LocalRc provides palette,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        val c = LocalRc.current
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
                // available, else the bundled default (tinted black for the 1-bit print).
                val logoBitmap = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(250.dp).padding(bottom = 8.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(c.ink),
                        modifier = Modifier.size(250.dp).padding(bottom = 8.dp),
                    )
                }
                if (companyName.isNotBlank()) {
                    Text(
                        text = companyName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = c.ink,
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
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
                TypeTag(label = typeLabel(state.type), color = typeColor(state.type))
            }

            SepDash()

            // Invoice meta
            KvRow(stringResource(Res.string.voucher_detail_number), "#${state.number}", valueColor = c.accent)
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
            ItemColHeader()

            // Items (purchased, then gift lines — each a normal item at 100% discount)
            state.lines.forEach { line ->
                ReceiptItemRow(line, t.amountDecimals)
            }
            state.freeLines.forEach { line ->
                ReceiptItemRow(line, t.amountDecimals)
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
            if (state.appliedOffers.isNotEmpty()) {
                state.appliedOffers.forEach { offer ->
                    TotRow(offer.name, "- ${money(offer.discountAmount, t)}", valueColor = c.negative)
                }
                val offersTotal = state.appliedOffers.sumOf { it.discountAmount }
                TotRow(stringResource(Res.string.print_offers_total), "- ${money(offersTotal, t)}", valueColor = c.negative)
            } else {
                val lineDiscount = state.lines.sumOf { it.qty * it.unitPrice * it.discountPct } + freeGross
                if (lineDiscount > 0.0005) {
                    TotRow(stringResource(Res.string.print_line_discount), "- ${money(lineDiscount, t)}", valueColor = c.negative)
                }
                if (state.discountAmount > 0.0005) {
                    TotRow(stringResource(Res.string.print_total_discount), "- ${money(state.discountAmount, t)}", valueColor = c.negative)
                }
            }
            if (state.taxAmount > 0.0) {
                TotRow(taxTotalLabel(state.lines), "+ ${money(state.taxAmount, t)}", valueColor = c.warn)
            }
            TotRow(
                stringResource(Res.string.print_item_count),
                (state.lines.sumOf { it.qty } + state.freeLines.sumOf { it.qty }).let {
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
                    .border(2.dp, c.ink),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.voucher_detail_total),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = c.ink,
                )
                Text(
                    text = money(state.total, t),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = c.accent,
                    style = LtrNum,
                )
            }

            // Notes
            state.notes?.let { notes ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.print_notes_prefix, notes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.muted,
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(1.5.dp, c.ink, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "QR",
                        fontSize = 10.sp,
                        color = c.ink,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t.qrCaption,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.muted,
                    lineHeight = 14.sp,
                )
            }

            // Star separator
            Spacer(Modifier.height(8.dp))
            Text(
                text = "* * * * * * * *",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = c.faint,
                letterSpacing = 3.sp,
            )

            // Footer
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.print_footer_thanks), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = c.muted)
                Spacer(Modifier.height(2.dp))
                Text("Powered by 7Software", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.muted, lineHeight = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Item row ──────────────────────────────────────────────────────────────────

@Composable
private fun ItemColHeader() {
    val c = LocalRc.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        listOf(
            stringResource(Res.string.print_col_qty),
            stringResource(Res.string.print_col_unit),
            stringResource(Res.string.print_col_tax),
            stringResource(Res.string.print_col_price),
            stringResource(Res.string.print_col_total),
        ).forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Canvas(Modifier.fillMaxWidth().height(1.5.dp)) {
        drawLine(c.ink, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx())
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun ReceiptItemRow(line: InvoiceLine, amountDecimals: Int) {
    val c = LocalRc.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        // Product name — Arabic, always reads right-to-left (right-aligned) on the printout.
        Text(
            text = line.nameAr,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = c.ink,
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
        Row(modifier = Modifier.fillMaxWidth()) {
            val qty = formatQty(line.qty)
            val unit = line.unit.ifBlank { "—" }
            val taxPct = line.taxPctLabel()
            val price = formatAmount(line.unitPrice, amountDecimals)
            val total = formatAmount(line.qty * line.unitPrice, amountDecimals)

            listOf(qty, unit, taxPct, price, total).forEachIndexed { idx, cell ->
                Text(
                    text = cell,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = if (idx == 4) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LtrNum,
                )
            }
        }
        // SKU
        Text(
            text = line.sku,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = c.faint,
        )
    }
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dashLen = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(c.faint, Offset(x, 0f), Offset((x + dashLen).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dashLen + gap
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SepSolid() {
    val c = LocalRc.current
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(c.ink, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SepDash() {
    val c = LocalRc.current
    Spacer(Modifier.height(7.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dash = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(c.faint, Offset(x, 0f), Offset((x + dash).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dash + gap
        }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun KvRow(key: String, value: String, valueColor: Color? = null) {
    val c = LocalRc.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = key, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.muted)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: c.ink,
            style = LtrNum,
        )
    }
}

@Composable
private fun TotRow(label: String, value: String, valueColor: Color? = null) {
    val c = LocalRc.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.muted)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: c.ink,
            style = LtrNum,
        )
    }
}

@Composable
private fun TypeTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
    }
}

/** Payment type in the header info block: label right, value in a 1.5px outlined box (no fill). */
@Composable
private fun PaymentTypeHeaderRow(label: String, value: String) {
    val c = LocalRc.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.muted)
        Box(
            modifier = Modifier
                .border(2.dp, c.ink, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = c.ink)
        }
    }
}

/** Payment type in the totals block: plain bold, no box, to keep the totals calm. */
@Composable
private fun PaymentTypeFooterRow(label: String, value: String) {
    val c = LocalRc.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.muted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
    }
}

@Composable
private fun SignatureBox(label: String) {
    val c = LocalRc.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
        Spacer(Modifier.height(16.dp))
        Canvas(Modifier.fillMaxWidth().height(1.5.dp)) {
            drawLine(c.faint, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx())
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

@Composable
private fun typeColor(type: String): Color {
    val c = LocalRc.current
    return when (type) {
        "SALE"    -> c.sale
        "RETURN"  -> c.ret
        "REQUEST" -> c.request
        else      -> c.muted
    }
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
