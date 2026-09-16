package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.designsystem.components.*
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

// The thermal slip has no palette of its own any more: black ink on white paper,
// out of ThermalInk. The greys that used to live here (#111111 ink, #555555 for
// labels, #DDDDDD hairlines) were carrying the receipt's hierarchy, and a 1-bit
// head cannot print any of them — it dithers them into speckle. Hierarchy is
// size, weight and rules from here on. The A4 page below keeps its own colours;
// it is read on a screen or printed by a real printer.

@Composable
fun ReceiptDetailScreen(
    paymentId: String,
    onBack: () -> Unit,
    viewModel: ReceiptDetailViewModel = koinViewModel { parametersOf(paymentId) },
) {
    val state by viewModel.state.collectAsState()
    val entity = state.entity
    // The thermal slip is captured off-screen at head resolution (see ThermalCapture
    // below), not read back off the preview. The preview's pixel width is the paper's
    // dp times whatever density the phone happens to have — a third of the dots on a
    // Sunmi terminal as on a modern phone, for the same 80mm of roll.
    val thermalLayer = rememberGraphicsLayer()
    // The A4 page is rendered off-screen; this layer captures it for "Share as PDF".
    val a4Layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    // Capture the on-screen receipt document and hand it to the VM as PNG bytes.
    suspend fun captureAndPrint() {
        val bitmap = thermalLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.print(png)
    }

    // Auto-print once a connection is established from the dialog.
    LaunchedEffect(state.pendingPrint, state.printerState) {
        if (state.pendingPrint && state.printerState is PrinterState.Connected) captureAndPrint()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_back), contentDescription = null, tint = Fv.TextHigh, modifier = Modifier.size(22.dp))
                }
                // The title takes the slack and truncates; the two action buttons keep their
                // full width. Sized the other way round, a narrow phone pushed "Thermal Print"
                // off the edge of the bar the moment Share joined it.
                Text(
                    stringResource(Res.string.receipt_voucher_title),
                    modifier = Modifier.weight(1f),
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entity != null) {
                    // Share sends the A4 page, not the thermal slip: a receipt that leaves
                    // the phone is read on a screen or filed, where a 58mm-wide strip is
                    // useless. Printing keeps the slip. Outlined, so the thermal button
                    // stays the primary action for a rep standing at the counter.
                    Surface(
                        onClick = {
                            scope.launch {
                                val bmp = a4Layer.toImageBitmap()
                                pdfHelper.shareAsPdf(bmp, entity.number, a4 = true)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Fv.Blue),
                    ) {
                        Text(
                            stringResource(Res.string.print_action_share_pdf),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Fv.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Surface(
                        onClick = {
                            if (state.printerState is PrinterState.Connected) scope.launch { captureAndPrint() }
                            else viewModel.requestConnectThenPrint()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Fv.Blue,
                    ) {
                        Text(
                            stringResource(Res.string.printer_thermal_print),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            state.printMessageAr?.let { msg ->
                Text(
                    msg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Fv.Amber, fontSize = 12.sp, textAlign = TextAlign.Center,
                )
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fv.Blue)
                }
                entity == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.receipt_detail_not_found), color = Fv.TextMid)
                }
                // Fixed-width paper — see requiredWidth below — so a screen narrower
                // than it pans rather than clipping a column off the edge.
                else -> Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // On-screen preview only — it records nothing. The rounded corner is
                    // chrome for the phone; the captured paper below has square corners,
                    // because a rounded corner is a grey arc to a 1-bit head.
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .requiredWidth(340.dp)
                            .background(ThermalInk.Paper, RoundedCornerShape(4.dp)),
                    ) {
                        PaymentReceiptDocument(entity, state)
                    }
                    Spacer(Modifier.height(24.dp))

                    // The print source: the same paper, drawn off-screen at 576 dots
                    // across regardless of this screen's density. 340.dp is the width
                    // the slip's layout was written against, so it stays 340.dp here.
                    ThermalCapture(layer = thermalLayer, paperDp = 340.dp) {
                        PaymentReceiptDocument(entity, state)
                    }

                    // Off-screen A4 page — captured for "Share as PDF" only, never painted.
                    //
                    // The outer layout measures it at its natural size (fixed width, UNBOUNDED
                    // height) then reports zero size to the parent, so it takes up no room on
                    // screen while still being drawn full-size into the layer. A size(0) wrapper
                    // would clamp the child to 0 height and toImageBitmap() would fail on a 0x0
                    // layer. Same construction as VoucherPrintScreen's.
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
                                    a4Layer.record { this@drawWithContent.drawContent() }
                                },
                        ) {
                            ReceiptA4Document(entity, state)
                        }
                    }
                }
            }
        }

        if (state.showConnectDialog) {
            PrinterConnectDialog(
                printerState = state.printerState,
                connectType = state.connectType,
                connectAddress = state.connectAddress,
                discoveredDevices = state.discoveredDevices,
                onTypeSelected = viewModel::connectTypeSelected,
                connectLanguage = state.connectLanguage,
                onLanguageSelected = viewModel::printerLanguageSelected,
                onAddressChanged = viewModel::connectAddressChanged,
                onDeviceSelected = viewModel::deviceSelected,
                onRefresh = viewModel::refreshDevices,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onDismiss = viewModel::dismissConnectDialog,
            )
        }
    }
}

/** The money-only cash / cheque receipt (سند قبض) — black on white for the printer. */
@Composable
private fun PaymentReceiptDocument(entity: PaymentEntity, state: ReceiptDetailState) {
    val methodLabel = when (entity.method) {
        "CASH" -> "نقدي"
        "CHEQUE" -> "شيك"
        "TRANSFER" -> "حوالة"
        else -> entity.method
    }
    // 14dp side margins rather than 18dp: the type is 40% larger than it was and the
    // rows need the width back. On 340dp of paper that still leaves a clear margin.
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 20.dp)) {
        // Header — company logo (own logo cached from /company-info, else bundled default).
        // Capped at 110dp: an uploaded logo is a continuous-tone photograph, and at 140dp
        // it was handing the head a greyscale image across a third of the roll. The tint
        // stays on the bundled mark — it is already monochrome — but tinting does not
        // make an image 1-bit, so the cap applies to both.
        val logoBitmap = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(110.dp).padding(bottom = 8.dp),
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.voucher_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ThermalInk.Ink),
                modifier = Modifier.align(Alignment.CenterHorizontally).size(110.dp).padding(bottom = 8.dp),
            )
        }
        Text(
            state.companyNameAr.ifBlank { "فان فلو" },
            modifier = Modifier.fillMaxWidth(),
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_COMPANY.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        // The document title was a small grey line, which is the one thing a thermal head
        // cannot render at all. Knocked out white on a solid black band instead: both
        // halves are 1-bit, and it reads as a title from across a counter.
        Text(
            "سند قبض",
            modifier = Modifier.fillMaxWidth().background(ThermalInk.Ink).padding(vertical = 5.dp),
            color = ThermalInk.Paper,
            fontSize = ThermalInk.FS_TITLE.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        HeavyRule()
        Spacer(Modifier.height(10.dp))

        DocRow("رقم السند", entity.number)
        DocRow("التاريخ", entity.createdAt.toDateTimeString())
        DocRow("العميل", state.customerNameAr.ifBlank { state.customerCode })
        if (state.salesmanNameAr.isNotBlank()) DocRow("المندوب", state.salesmanNameAr)
        DocRow("طريقة الدفع", methodLabel)

        Spacer(Modifier.height(10.dp))
        Rule()
        Spacer(Modifier.height(10.dp))

        // Amount — the money
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "المبلغ",
                color = ThermalInk.Ink,
                fontSize = ThermalInk.FS_TOTAL.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                entity.amount.formatJod(AppLanguage.AR),
                modifier = Modifier.padding(start = 8.dp),
                color = ThermalInk.Ink,
                fontSize = ThermalInk.FS_TOTAL.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // Cheque details
        if (entity.method == "CHEQUE") {
            Spacer(Modifier.height(10.dp))
            HeavyRule()
            Spacer(Modifier.height(8.dp))
            Text(
                "بيانات الشيك",
                color = ThermalInk.Ink,
                fontSize = ThermalInk.FS_SECTION.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            entity.chequeBank?.let { DocRow("البنك", it) }
            entity.chequeNumber?.let { DocRow("رقم الشيك", it) }
            entity.chequeDate?.let { DocRow("تاريخ الاستحقاق", it.toDateString()) }
        }

        val transferRef = entity.transferRef
        if (entity.method == "TRANSFER" && !transferRef.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            DocRow("رقم المرجع", transferRef)
        }

        val notes = entity.notes
        if (!notes.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            DocRow("ملاحظة", notes)
        }

        Spacer(Modifier.height(14.dp))
        Rule()
        Spacer(Modifier.height(10.dp))
        // The courtesy line sits at the floor of the scale — the smallest type a thermal
        // head still resolves Arabic at — and is bold like everything else on the paper.
        Text(
            "شكراً لتعاملكم معنا",
            modifier = Modifier.fillMaxWidth(),
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_MIN.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A label/value line on the slip.
 *
 * Label and value used to be told apart by grey against black; now both are black
 * and the value carries the extra weight. The label takes the slack and wraps, so
 * a long value is never squeezed into a second line of its own.
 */
@Composable
private fun DocRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            modifier = Modifier.weight(1f, fill = false),
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            value,
            modifier = Modifier.padding(start = 8.dp),
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_ROW.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** The ordinary separator. Was a 1dp #DDDDDD hairline, which printed as nothing at all. */
@Composable
private fun Rule() {
    HorizontalDivider(color = ThermalInk.Ink, thickness = ThermalInk.RuleThickness)
}

/** The heavier rule that closes the header and opens the cheque block. */
@Composable
private fun HeavyRule() {
    HorizontalDivider(color = ThermalInk.Ink, thickness = 3.dp)
}
