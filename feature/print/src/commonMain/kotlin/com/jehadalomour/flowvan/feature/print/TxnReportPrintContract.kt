package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.data.repository.TransactionReport
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

/**
 * The printable تقرير الحركات. Everything on it comes from the server, like the
 * screen it was opened from — a printed report that quietly used the local cache
 * would be a different document with the same title.
 */
data class TxnReportPrintState(
    val isLoading: Boolean = true,
    val customerId: String = "",
    val customerNameAr: String = "",
    val customerCode: String = "",
    val customerPhone: String = "",
    val salesmanNameAr: String = "",
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val printedAt: Long = 0L,
    val report: TransactionReport = TransactionReport(),
    /** Set when the server could not be reached; the paper is not offered at all. */
    val errorAr: String? = null,

    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    val companyLogo: String = "",

    // ── Thermal printer ──────────────────────────────────────────────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
)

sealed interface TxnReportPrintEvent {
    data object RequestConnectThenPrint : TxnReportPrintEvent
    data object DismissConnectDialog : TxnReportPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : TxnReportPrintEvent
    data class ConnectAddressChanged(val address: String) : TxnReportPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : TxnReportPrintEvent
    data object RefreshDevices : TxnReportPrintEvent
    data object Connect : TxnReportPrintEvent
    data object Disconnect : TxnReportPrintEvent
    data class Print(val png: ByteArray) : TxnReportPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && png.contentEquals(other.png))
        override fun hashCode() = png.contentHashCode()
    }
    data object DismissMessage : TxnReportPrintEvent
}
