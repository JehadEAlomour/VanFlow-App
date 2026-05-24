package com.jehadalomour.flowvan.screens.print

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.platform.rememberPdfShareHelper
import com.jehadalomour.flowvan.platform.printer.PrinterConnectDialog
import com.jehadalomour.flowvan.platform.printer.toPngBytes
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import com.jehadalomour.flowvan.shared.domain.printer.PrinterState
import com.jehadalomour.flowvan.shared.presentation.feature.print.VoucherPrintEvent
import com.jehadalomour.flowvan.shared.presentation.feature.print.VoucherPrintState
import com.jehadalomour.flowvan.shared.presentation.feature.print.VoucherPrintViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
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
    Column(modifier = Modifier.background(RcBg).padding(14.dp)) {

        // Company header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "شركتي للتجارة",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Dark,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = buildString {
                    if (state.branch.isNotBlank()) append("${state.branch} | ")
                    append("ر.ض: 1234567890")
                },
                fontSize = 9.sp,
                color = SubText,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
            )
        }

        SepSolid()

        // Voucher type + payment method tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeTag(label = typeLabel(state.type), color = typeColor(state.type))
            paymentLabel(state.paymentMethod)?.let { label ->
                Spacer(Modifier.size(8.dp))
                TypeTag(label = label, color = paymentColor(state.paymentMethod))
            }
        }

        SepDash()

        // Invoice meta
        KvRow(stringResource(Res.string.voucher_detail_number), "#${state.number}", valueColor = Blue)
        KvRow(stringResource(Res.string.voucher_detail_date), state.createdAt.toReceiptDateStr())
        KvRow(stringResource(Res.string.print_salesman), state.salesmanNameAr)

        SepDash()

        // Customer
        KvRow(stringResource(Res.string.print_customer), state.customerNameAr)
        KvRow(stringResource(Res.string.print_customer_code), state.customerCode)
        state.customerTaxNumber?.let { KvRow(stringResource(Res.string.print_customer_tax_number), it) }

        SepSolid()

        // Column headers
        ItemColHeader()

        // Items
        state.lines.forEach { line ->
            ReceiptItemRow(line)
        }

        SepDash()

        // Totals
        TotRow(stringResource(Res.string.voucher_detail_subtotal), state.subtotal.formatJod())
        if (state.discountAmount > 0.0) {
            TotRow(stringResource(Res.string.print_total_discount), "- ${state.discountAmount.formatJod()}", valueColor = Red)
        }
        if (state.taxAmount > 0.0) {
            TotRow(stringResource(Res.string.voucher_detail_tax), "+ ${state.taxAmount.formatJod()}", valueColor = Amber)
        }

        // Grand total
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.5.dp, Dark),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.voucher_detail_total),
                modifier = Modifier.padding(horizontal = 10.dp),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Dark,
            )
            Text(
                text = state.total.formatJod(),
                modifier = Modifier.padding(horizontal = 10.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Blue,
                style = TextStyle(textDirection = TextDirection.Ltr),
            )
        }

        // Notes
        state.notes?.let { notes ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.print_notes_prefix, notes),
                fontSize = 8.5.sp,
                color = SubText,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Signature boxes
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SignatureBox(stringResource(Res.string.print_signature_recipient))
            SignatureBox(stringResource(Res.string.print_signature_stamp))
        }

        // QR placeholder
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.CenterHorizontally)
                .border(1.5.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "QR\nCode",
                fontSize = 9.sp,
                color = DivLight,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
            )
        }

        // Star separator
        Spacer(Modifier.height(6.dp))
        Text(
            text = "* * * * * * * *",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = DivLight,
            letterSpacing = 3.sp,
        )

        // Footer
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.print_footer_thanks), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SubText)
            Text(stringResource(Res.string.print_footer_legal), fontSize = 8.5.sp, color = DivLight, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Item row ──────────────────────────────────────────────────────────────────

@Composable
private fun ItemColHeader() {
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
            stringResource(Res.string.print_col_discount),
            stringResource(Res.string.print_col_total),
        ).forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = SubText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(Dark, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun ReceiptItemRow(line: InvoiceLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        // Product name
        Text(
            text = line.nameAr,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Dark,
            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
        )
        // 6-column data grid
        Row(modifier = Modifier.fillMaxWidth()) {
            val qty = formatQty(line.qty)
            val unit = line.unit.ifBlank { "—" }
            val taxPct = line.taxPctLabel()
            val price = formatPrice(line.unitPrice)
            val disc = if (line.discountPct > 0.0) "${(line.discountPct * 100).toInt()}%" else "—"
            val total = formatPrice(line.lineTotal)

            listOf(qty, unit, taxPct, price, disc, total).forEachIndexed { idx, cell ->
                Text(
                    text = cell,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 8.5.sp,
                    fontWeight = if (idx == 5) FontWeight.Bold else FontWeight.Normal,
                    color = when (idx) {
                        4 -> if (line.discountPct > 0.0) Red else Dark
                        else -> Dark
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // SKU
        Text(
            text = line.sku,
            fontSize = 8.sp,
            color = DivLight,
        )
    }
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dashLen = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(DivItem, Offset(x, 0f), Offset((x + dashLen).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dashLen + gap
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SepSolid() {
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(Dark, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SepDash() {
    Spacer(Modifier.height(7.dp))
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val dash = 4.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(DivLight, Offset(x, 0f), Offset((x + dash).coerceAtMost(size.width), 0f), strokeWidth = 1.dp.toPx())
            x += dash + gap
        }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun KvRow(key: String, value: String, valueColor: Color = Dark) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = key, fontSize = 10.sp, color = SubText)
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            style = TextStyle(textDirection = TextDirection.Ltr),
        )
    }
}

@Composable
private fun TotRow(label: String, value: String, valueColor: Color = Dark) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 10.sp, color = SubText)
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            style = TextStyle(textDirection = TextDirection.Ltr),
        )
    }
}

@Composable
private fun TypeTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = color)
    }
}

@Composable
private fun SignatureBox(label: String) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 8.sp, color = SubText)
        Spacer(Modifier.height(16.dp))
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(DivLight, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
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

private fun typeColor(type: String) = when (type) {
    "SALE"    -> Green
    "RETURN"  -> Red
    "REQUEST" -> Blue
    else      -> SubText
}

@Composable
private fun paymentLabel(method: String?): String? = when (method) {
    "CASH"     -> stringResource(Res.string.print_payment_cash)
    "CHEQUE"   -> stringResource(Res.string.payment_method_cheque)
    "TRANSFER" -> stringResource(Res.string.print_payment_transfer)
    "CREDIT"   -> stringResource(Res.string.print_payment_credit)
    else       -> null
}

private fun paymentColor(method: String?) = when (method) {
    "CREDIT" -> Amber
    else     -> SubText
}

@Composable
private fun InvoiceLine.taxPctLabel(): String = when (taxType) {
    "INCLUSIVE" -> stringResource(Res.string.print_tax_inclusive)
    "TAXABLE"   -> if (taxRate > 0.0) "${(taxRate * 100).toInt()}%" else "—"
    else        -> "—"
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)

private fun formatPrice(price: Double): String = "%.3f".format(price)

private fun Long.toReceiptDateStr(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/" +
        "${dt.year}  " +
        "${dt.hour.toString().padStart(2, '0')}:" +
        dt.minute.toString().padStart(2, '0')
}
