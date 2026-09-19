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

/**
 * The van's own stock, on paper.
 *
 * Drawn on the same roll as the statement and the reports, through the same
 * [TxnReportPaperKit] — a shop and an office receiving any of them should see
 * one company's documents rather than four templates.
 *
 * Everything on it comes off the device (see [VanStockPrintViewModel]); no
 * endpoint was added for this, and none is contacted when it prints. A rep
 * counting his van is standing in a yard, and a stock sheet that needs a signal
 * is one he cannot have at the moment he needs it.
 *
 * The paper is drawn twice, like every other receipt here: once for the screen,
 * and once off-screen through [ThermalCapture] at the head's own resolution.
 */
@Composable
fun VanStockPrintScreen(
    onBack: () -> Unit,
    viewModel: VanStockPrintViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val pdfHelper = rememberPdfShareHelper()

    val docName = remember(state.printedAt) {
        "van-stock-${state.printedAt.txnDate().replace('/', '-')}"
    }

    suspend fun captureAndPrint() {
        val bitmap = graphicsLayer.toImageBitmap()
        val png = withContext(Dispatchers.Default) { bitmap.toPngBytes() }
        viewModel.onEvent(VanStockPrintEvent.Print(png))
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
                        stringResource(Res.string.van_stock_print),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        state.printedAt.txnDateTime(),
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
                        viewModel.onEvent(VanStockPrintEvent.RequestConnectThenPrint)
                    }
                },
            )
            Spacer(Modifier.size(10.dp))
            // Shares the roll as its own page rather than shrunk onto A4: a van
            // carrying eighty lines scaled to fit one sheet is type nobody can
            // count against.
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
                onTypeSelected = { viewModel.onEvent(VanStockPrintEvent.ConnectTypeSelected(it)) },
                connectLanguage = state.connectLanguage,
                onLanguageSelected = { viewModel.onEvent(VanStockPrintEvent.PrinterLanguageSelected(it)) },
                onAddressChanged = { viewModel.onEvent(VanStockPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(VanStockPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(VanStockPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(VanStockPrintEvent.Connect) },
                onDisconnect = { viewModel.onEvent(VanStockPrintEvent.Disconnect) },
                onDismiss = { viewModel.onEvent(VanStockPrintEvent.DismissConnectDialog) },
            )
        }

        // Fixed-width paper: a narrower screen pans rather than clipping the
        // value column off the edge.
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Preview only — drawn at the phone's density, records nothing.
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .requiredWidth(TxnPaperWidth)
                    .shadow(8.dp, RoundedCornerShape(4.dp))
                    .background(TxnPaperBg, RoundedCornerShape(4.dp)),
            ) {
                Column {
                    TxnTear()
                    VanStockBody(state)
                    TxnTear(flipped = true)
                }
            }

            // The print source: the same body at the head's own resolution, so
            // 384dp of paper is 576 dots on the Sunmi terminal as well as on a
            // modern handset. The torn edges stay behind — they are grey, and
            // grey is the one thing a 1-bit head cannot print.
            ThermalCapture(layer = graphicsLayer, paperDp = TxnPaperWidth) {
                VanStockBody(state)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── The paper ─────────────────────────────────────────────────────────────────

// Column budget. The name takes what it can get: an item is identified on this
// sheet by its name first and its code second, and the code is printed in full
// on its own line below rather than squeezed into a column.
private const val VS_COL_ITEM = 2.4f
private const val VS_COL_QTY = 1.0f
private const val VS_COL_VALUE = 1.3f

@Composable
private fun VanStockBody(state: VanStockPrintState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    // Untinted: an uploaded logo is a photograph with no
                    // transparency, and tinting one prints a solid black box.
                    // See StatementPrintScreen.
                    Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(TxnLogoSize))
                } else {
                    Image(
                        painter = painterResource(Res.drawable.voucher_logo),
                        contentDescription = null,
                        modifier = Modifier.size(TxnLogoSize),
                        colorFilter = ColorFilter.tint(ThermalInk.Ink),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            TxnCenter(state.companyNameAr, TXN_FS_COMPANY, bold = true)
            if (state.companyNameEn.isNotBlank()) TxnCenter(state.companyNameEn, TXN_FS_SUB)
            if (state.companyTaxNumber.isNotBlank()) {
                TxnCenter(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    TXN_FS_SUB,
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(ThermalInk.Ink).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.van_stock_print),
                    color = ThermalInk.Paper,
                    fontSize = TXN_FS_TITLE.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(8.dp))

            // Whoever is holding the phone is who the office asks about this
            // count, and when they counted.
            if (state.salesmanNameAr.isNotBlank()) {
                TxnInfo(stringResource(Res.string.statement_salesman), state.salesmanNameAr)
            }
            TxnInfo(stringResource(Res.string.statement_printed_at), state.printedAt.txnDateTime())

            Spacer(Modifier.height(8.dp))
            TxnRule()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TxnHead(stringResource(Res.string.van_stock_col_item), VS_COL_ITEM)
                TxnHead(stringResource(Res.string.van_stock_col_qty), VS_COL_QTY)
                TxnHead(stringResource(Res.string.van_stock_col_value), VS_COL_VALUE)
            }
            TxnRule()

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                TxnCenter(stringResource(Res.string.van_stock_print_empty), TXN_FS_ROW)
                Spacer(Modifier.height(10.dp))
            } else {
                state.rows.forEach { row ->
                    VanStockPaperRow(row)
                    TxnThinRule()
                }
            }

            Spacer(Modifier.height(6.dp))
            TxnRule()
            Spacer(Modifier.height(6.dp))

            // Lines and pieces both, because they answer different questions:
            // how long the sheet is, and how much there is to count. A van with
            // three items and three hundred pieces is not a van with three of
            // anything.
            TxnTotal(stringResource(Res.string.van_stock_stat_items), state.itemCount.toString())
            TxnTotal(stringResource(Res.string.van_stock_stat_total_qty), state.totalQty.toString())

            Spacer(Modifier.height(6.dp))
            TxnBoxedTotal(
                stringResource(Res.string.van_stock_stat_total_value),
                state.totalValue.txnJod(),
            )

            Spacer(Modifier.height(14.dp))
            // A stock count is signed: this sheet is what a rep and whoever
            // checks the van agree it held at that moment.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VanStockSignature(stringResource(Res.string.statement_sign_salesman), Modifier.weight(1f))
                VanStockSignature(stringResource(Res.string.van_stock_sign_checker), Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            TxnCenter(stringResource(Res.string.print_footer_thanks), ThermalInk.FS_MIN)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VanStockPaperRow(row: VanStockPrintRow) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The name is the column, not a cell: it is Arabic, so it is neither
            // LTR nor allowed to clip, and TxnCell is both.
            Text(
                text = row.name,
                modifier = Modifier.weight(VS_COL_ITEM),
                color = ThermalInk.Ink,
                fontSize = TXN_FS_ROW.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TxnCell(row.qty.toString(), VS_COL_QTY, bold = true)
            TxnCell(row.value.txnJod(), VS_COL_VALUE)
        }
        // Code and unit on a line of their own, left-to-right. The code is what
        // a rep reads back down the phone, so it is never allowed to be the
        // thing a narrow column cut in half.
        Text(
            text = if (row.unit.isBlank()) row.itemNumber else "${row.itemNumber} · ${row.unit}",
            modifier = Modifier.fillMaxWidth(),
            color = ThermalInk.Ink,
            fontSize = TXN_FS_SUB_ROW.sp,
            fontWeight = FontWeight.Bold,
            style = TxnLtr,
            textAlign = TextAlign.Left,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A ruled line to sign on. Solid ink — a hairline does not survive the head. */
@Composable
private fun VanStockSignature(label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness).background(ThermalInk.Ink))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = ThermalInk.Ink,
            fontSize = ThermalInk.FS_MIN.sp,
            fontWeight = TxnWeight,
            textAlign = TextAlign.Center,
        )
    }
}
