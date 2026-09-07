package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

/**
 * The printable الكشف اليومي.
 *
 * The slip a rep hands over when closing a round: what was sold, what came back,
 * what was collected, and — the line the handover actually turns on — how much
 * cash should be in the bag. Every figure here is computed the same way
 * CashFlowReportViewModel computes it for the screen, because a printout that
 * disagrees with the screen it was opened from is the one bug this document
 * cannot survive.
 *
 * Built from the local database, like the sales report and unlike the server-backed
 * تقرير الحركات: these are this device's own vouchers, so the paper is complete
 * offline — which is exactly where a round gets closed.
 */
data class CashFlowPrintState(
    val isLoading: Boolean = true,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val printedAt: Long = 0L,
    val salesmanNameAr: String = "",

    // ── Sales ────────────────────────────────────────────────────────────────
    val salesCashTotal: Double = 0.0,
    val salesCreditTotal: Double = 0.0,
    val salesTotal: Double = 0.0,
    val salesCount: Int = 0,

    // ── Returns ──────────────────────────────────────────────────────────────
    val returnsCashTotal: Double = 0.0,
    val returnsCreditTotal: Double = 0.0,
    val returnsTotal: Double = 0.0,
    val returnsCount: Int = 0,

    // ── Collections ──────────────────────────────────────────────────────────
    val collectionsCashTotal: Double = 0.0,
    val collectionsChequeTotal: Double = 0.0,
    val collectionsTotal: Double = 0.0,
    val collectionsCount: Int = 0,

    /** Cash that should physically be in the bag: sales cash + collections cash − returns cash. */
    val totalCash: Double = 0.0,

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

sealed interface CashFlowPrintEvent {
    data object RequestConnectThenPrint : CashFlowPrintEvent
    data object DismissConnectDialog : CashFlowPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : CashFlowPrintEvent
    data class ConnectAddressChanged(val address: String) : CashFlowPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : CashFlowPrintEvent
    data object RefreshDevices : CashFlowPrintEvent
    data object Connect : CashFlowPrintEvent
    data object Disconnect : CashFlowPrintEvent
    data class Print(val png: ByteArray) : CashFlowPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && png.contentEquals(other.png))
        override fun hashCode() = png.contentHashCode()
    }
    data object DismissMessage : CashFlowPrintEvent
}
