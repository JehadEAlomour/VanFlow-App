package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenBg = Color(0xFFD1D5DB)

/**
 * Bulk print of every SALE invoice's detail (with the tax QR) for a date range.
 * The VM walks the invoices one at a time; here we render the current one into an
 * off-screen layer, capture it to a PNG, and hand it back for printing.
 */
@Composable
fun SalesBulkPrintScreen(
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit,
    viewModel: SalesBulkPrintViewModel = koinViewModel { parametersOf(fromMillis, toMillis) },
) {
    val state by viewModel.state.collectAsState()
    val thermalLayer = rememberGraphicsLayer()

    // Capture the current invoice once it has composed and drawn into the layer.
    LaunchedEffect(state.captureNonce) {
        val nonce = state.captureNonce
        if (state.current == null || nonce == 0) return@LaunchedEffect
        withFrameNanos { }
        delay(150) // let the off-screen receipt lay out and draw into the layer
        val png = runCatching {
            val bmp = thermalLayer.toImageBitmap()
            withContext(Dispatchers.Default) { bmp.toPngBytes() }
        }.getOrNull() ?: return@LaunchedEffect
        viewModel.onEvent(SalesBulkPrintEvent.PngCaptured(png, nonce))
    }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹", fontSize = 22.sp) }
            Spacer(Modifier.size(4.dp))
            Text(
                stringResource(Res.string.bulk_print_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        if (state.total > 0 && state.phase != BulkPhase.EMPTY) {
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.printedCount.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.phase) {
                    BulkPhase.LOADING -> {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.bulk_print_preparing))
                    }
                    BulkPhase.EMPTY -> Text(stringResource(Res.string.bulk_print_empty))
                    BulkPhase.NEED_CONNECT -> {
                        Text(stringResource(Res.string.bulk_print_need_connect))
                        Button(onClick = { viewModel.onEvent(SalesBulkPrintEvent.RequestConnect) }) {
                            Text(stringResource(Res.string.printer_connect))
                        }
                    }
                    BulkPhase.PRINTING -> {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.bulk_print_progress, state.printedCount + 1, state.total))
                    }
                    BulkPhase.DONE -> Text(stringResource(Res.string.bulk_print_done, state.printedCount))
                    BulkPhase.ERROR -> {
                        Text(state.message ?: stringResource(Res.string.bulk_print_error))
                        Button(onClick = { viewModel.onEvent(SalesBulkPrintEvent.Retry) }) {
                            Text(stringResource(Res.string.bulk_print_retry))
                        }
                    }
                }
                if (state.message != null && state.phase != BulkPhase.ERROR) {
                    Text(state.message!!, color = Color(0xFFD94040), fontSize = 12.sp)
                }
            }
        }

        // Off-screen render of the current invoice — drawn at full size into the
        // layer but reported as 0×0 so it never appears on screen.
        state.current?.let { cur ->
            Box(
                modifier = Modifier.layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    layout(0, 0) { placeable.place(0, 0) }
                },
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(320.dp)
                        .background(RcBg)
                        .drawWithContent { thermalLayer.record { this@drawWithContent.drawContent() } },
                ) {
                    Column {
                        ReceiptTear()
                        ReceiptBody(cur, cur.lines)
                        ReceiptTear(flipped = true)
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
                onTypeSelected = { viewModel.onEvent(SalesBulkPrintEvent.ConnectTypeSelected(it)) },
                onAddressChanged = { viewModel.onEvent(SalesBulkPrintEvent.ConnectAddressChanged(it)) },
                onDeviceSelected = { viewModel.onEvent(SalesBulkPrintEvent.DeviceSelected(it)) },
                onRefresh = { viewModel.onEvent(SalesBulkPrintEvent.RefreshDevices) },
                onConnect = { viewModel.onEvent(SalesBulkPrintEvent.Connect) },
                onDisconnect = { /* stay connected for the batch */ },
                onDismiss = { viewModel.onEvent(SalesBulkPrintEvent.DismissConnectDialog) },
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
