package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

sealed class VoucherSummaryEvent {
    // ── Date range ────────────────────────────────────────────────────────────
    data class SetFrom(val millis: Long) : VoucherSummaryEvent()
    data class SetTo(val millis: Long) : VoucherSummaryEvent()

    // ── Thermal printing (mirrors VoucherPrintEvent / EndOfDayEvent) ──────────
    /** Tapped print while disconnected — open the dialog and print once connected. */
    data object RequestConnectThenPrint : VoucherSummaryEvent()
    data object DismissConnectDialog : VoucherSummaryEvent()
    data class ConnectTypeSelected(val type: PrinterType) : VoucherSummaryEvent()
    data class ConnectAddressChanged(val address: String) : VoucherSummaryEvent()
    data class DeviceSelected(val target: PrinterTarget) : VoucherSummaryEvent()
    data object RefreshDevices : VoucherSummaryEvent()
    data object Connect : VoucherSummaryEvent()
    data object Disconnect : VoucherSummaryEvent()

    /** UI captured the summary as PNG bytes; send it to the printer. */
    data class Print(val receiptPng: ByteArray) : VoucherSummaryEvent() {
        override fun equals(other: Any?) =
            this === other || (other is Print && receiptPng.contentEquals(other.receiptPng))
        override fun hashCode() = receiptPng.contentHashCode()
    }
    data object DismissMessage : VoucherSummaryEvent()
}
