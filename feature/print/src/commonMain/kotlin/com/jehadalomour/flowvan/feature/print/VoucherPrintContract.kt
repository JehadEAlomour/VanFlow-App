package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

data class VoucherPrintState(
    val isLoading: Boolean = true,
    val invoiceId: String = "",
    val number: String = "",
    val type: String = "SALE",
    val paymentMethod: String? = null,
    val createdAt: Long = 0L,
    val customerNameAr: String = "",
    val customerCode: String = "",
    val customerTaxNumber: String? = null,
    val salesmanNameAr: String = "",
    val lines: List<InvoiceLine> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    val branch: String = "",

    // ── Thermal printer ──────────────────────────────────────────────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    /** Print the receipt automatically once a connection is established. */
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
)

/** All user intents on the print screen. */
sealed interface VoucherPrintEvent {
    /** Tapped "thermal print" while disconnected — open the dialog and print once connected. */
    data object RequestConnectThenPrint : VoucherPrintEvent
    data object DismissConnectDialog : VoucherPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : VoucherPrintEvent
    data class ConnectAddressChanged(val address: String) : VoucherPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : VoucherPrintEvent
    data object RefreshDevices : VoucherPrintEvent
    data object Connect : VoucherPrintEvent
    data object Disconnect : VoucherPrintEvent
    /** UI captured the receipt as PNG bytes; send it to the printer. */
    data class Print(val receiptPng: ByteArray) : VoucherPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && receiptPng.contentEquals(other.receiptPng))
        override fun hashCode() = receiptPng.contentHashCode()
    }
    data object DismissMessage : VoucherPrintEvent
}
