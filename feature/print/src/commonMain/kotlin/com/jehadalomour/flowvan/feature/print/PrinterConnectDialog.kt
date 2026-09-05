package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable, stateless dialog to pick a connection type and connect to a thermal printer.
 * All state and actions are owned by the caller's ViewModel — this is a dumb view.
 */
@Composable
fun PrinterConnectDialog(
    printerState: PrinterState,
    connectType: PrinterType,
    connectAddress: String,
    discoveredDevices: List<PrinterTarget>,
    onTypeSelected: (PrinterType) -> Unit,
    onAddressChanged: (String) -> Unit,
    onDeviceSelected: (PrinterTarget) -> Unit,
    onRefresh: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
    // The command language (ESC/POS vs Zebra CPCL). Optional so only the printer
    // SETUP screen shows the choice; the per-print screens inherit the saved setting.
    connectLanguage: PrinterLanguage? = null,
    onLanguageSelected: ((PrinterLanguage) -> Unit)? = null,
) {
    val connecting = printerState is PrinterState.Connecting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.printer_connect_title)) },
        confirmButton = {
            TextButton(onClick = onConnect, enabled = connectAddress.isNotBlank() && !connecting) {
                if (connecting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.printer_connect))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.printer_cancel)) }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {

                // Connection type chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Serial transport intentionally omitted: its native driver isn't 16 KB-aligned
                    // and is excluded from the APK. Bluetooth / USB / Network only.
                    TypeChip(PrinterType.BLUETOOTH, connectType, stringResource(Res.string.printer_type_bluetooth), onTypeSelected)
                    TypeChip(PrinterType.USB, connectType, stringResource(Res.string.printer_type_usb), onTypeSelected)
                    TypeChip(PrinterType.NETWORK, connectType, stringResource(Res.string.printer_type_network), onTypeSelected)
                }

                // Printer language (ESC/POS vs Zebra CPCL) — shown only where wired.
                if (connectLanguage != null && onLanguageSelected != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.printer_language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = connectLanguage == PrinterLanguage.ESCPOS,
                            onClick = { onLanguageSelected(PrinterLanguage.ESCPOS) },
                            label = { Text(stringResource(Res.string.printer_language_escpos), fontSize = 12.sp) },
                        )
                        FilterChip(
                            selected = connectLanguage == PrinterLanguage.CPCL,
                            onClick = { onLanguageSelected(PrinterLanguage.CPCL) },
                            label = { Text(stringResource(Res.string.printer_language_cpcl), fontSize = 12.sp) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(statusText(printerState), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))

                when (connectType) {
                    PrinterType.USB, PrinterType.SERIAL ->
                        DeviceListSection(discoveredDevices, connectAddress, onRefresh, onDeviceSelected)

                    PrinterType.BLUETOOTH -> {
                        // Paired printers first; manual MAC entry as a fallback.
                        DeviceListSection(discoveredDevices, connectAddress, onRefresh, onDeviceSelected)
                        Spacer(Modifier.height(8.dp))
                        AddressField(connectAddress, onAddressChanged, stringResource(Res.string.printer_address_bluetooth_hint))
                    }

                    PrinterType.NETWORK ->
                        AddressField(connectAddress, onAddressChanged, stringResource(Res.string.printer_address_network_hint))
                }

                if (printerState is PrinterState.Connected) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDisconnect) {
                        Text(stringResource(Res.string.printer_disconnect), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
    )
}

@Composable
private fun DeviceListSection(
    devices: List<PrinterTarget>,
    selectedAddress: String,
    onRefresh: () -> Unit,
    onDeviceSelected: (PrinterTarget) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(Res.string.printer_devices), fontSize = 13.sp)
        TextButton(onClick = onRefresh) { Text(stringResource(Res.string.printer_refresh)) }
    }
    if (devices.isEmpty()) {
        Text(
            stringResource(Res.string.printer_no_devices),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            devices.forEach { target ->
                DeviceRow(target.name, selected = target.address == selectedAddress) {
                    onDeviceSelected(target)
                }
            }
        }
    }
}

@Composable
private fun AddressField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
    )
}

@Composable
private fun TypeChip(type: PrinterType, selected: PrinterType, label: String, onSelect: (PrinterType) -> Unit) {
    FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(label, fontSize = 12.sp) })
}

@Composable
private fun DeviceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 13.sp)
    }
}

@Composable
private fun statusText(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}
