package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

sealed class EndOfDayEvent {
    data object OpenConfirmDialog : EndOfDayEvent()
    data object DismissConfirmDialog : EndOfDayEvent()
    data object ConfirmEndShift : EndOfDayEvent()

    // ── Thermal printing (mirrors VoucherPrintEvent) ──────────────────────────
    /** Tapped print while disconnected — open the dialog and print once connected. */
    data object RequestConnectThenPrint : EndOfDayEvent()
    data object DismissConnectDialog : EndOfDayEvent()
    data class ConnectTypeSelected(val type: PrinterType) : EndOfDayEvent()
    data class PrinterLanguageSelected(val language: PrinterLanguage) : EndOfDayEvent()
    data class ConnectAddressChanged(val address: String) : EndOfDayEvent()
    data class DeviceSelected(val target: PrinterTarget) : EndOfDayEvent()
    data object RefreshDevices : EndOfDayEvent()
    data object Connect : EndOfDayEvent()
    data object Disconnect : EndOfDayEvent()

    /** UI captured the summary as PNG bytes; send it to the printer. */
    data class Print(val receiptPng: ByteArray) : EndOfDayEvent() {
        override fun equals(other: Any?) =
            this === other || (other is Print && receiptPng.contentEquals(other.receiptPng))
        override fun hashCode() = receiptPng.contentHashCode()
    }
    data object DismissMessage : EndOfDayEvent()
}
