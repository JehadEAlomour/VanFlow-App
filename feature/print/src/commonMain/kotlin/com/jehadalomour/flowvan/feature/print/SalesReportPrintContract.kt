package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

/** One document on the printed تقرير المبيعات. */
data class SalesReportPrintRow(
    val number: String,
    val customerNameAr: String,
    val dateMillis: Long,
    /** SALE · RETURN · anything else is treated as a request. */
    val type: String,
    val total: Double,
    /** True for a credit sale; meaningless for returns and requests. */
    val isCredit: Boolean,
)

/**
 * The printable تقرير المبيعات.
 *
 * Built from the local database on purpose, unlike [TxnReportPrintState] which refuses
 * to print without the server. These are the rep's own vouchers, written on this
 * device — including ones not yet synced — so the paper is complete offline, which is
 * where a round is closed out.
 */
data class SalesReportPrintState(
    val isLoading: Boolean = true,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val printedAt: Long = 0L,
    val salesmanNameAr: String = "",

    val rows: List<SalesReportPrintRow> = emptyList(),
    val salesTotal: Double = 0.0,
    val returnsTotal: Double = 0.0,
    val requestsTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val creditTotal: Double = 0.0,
    val netTotal: Double = 0.0,
    val count: Int = 0,

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

sealed interface SalesReportPrintEvent {
    data object RequestConnectThenPrint : SalesReportPrintEvent
    data object DismissConnectDialog : SalesReportPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : SalesReportPrintEvent
    data class ConnectAddressChanged(val address: String) : SalesReportPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : SalesReportPrintEvent
    data object RefreshDevices : SalesReportPrintEvent
    data object Connect : SalesReportPrintEvent
    data object Disconnect : SalesReportPrintEvent
    data class Print(val png: ByteArray) : SalesReportPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && png.contentEquals(other.png))
        override fun hashCode() = png.contentHashCode()
    }
    data object DismissMessage : SalesReportPrintEvent
}
