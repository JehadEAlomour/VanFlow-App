package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.model.DailyKpi
import com.jehadalomour.flowvan.core.model.Shift
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

data class EndOfDayState(
    val kpi: DailyKpi? = null,
    val cashCollectedToday: Double = 0.0,
    val chequesCollectedToday: Double = 0.0,
    val transfersCollectedToday: Double = 0.0,
    /** SALE vouchers paid in cash today — cash into the drawer. */
    val cashSalesToday: Double = 0.0,
    /** Returns refunded in cash today (credit notes excluded) — cash out of the drawer. */
    val cashReturnsToday: Double = 0.0,
    val unsyncedInvoices: Int = 0,
    val unsyncedPayments: Int = 0,
    val activeShift: Shift? = null,
    val showConfirmDialog: Boolean = false,
    val isEnding: Boolean = false,
    val done: Boolean = false,

    // ── Receipt header (for the printed EOD summary) ──────────────────────────
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    val branch: String = "",
    val salesmanNameAr: String = "",
    /** Millis timestamp printed on the summary; captured when the screen loads. */
    val reportAt: Long = 0L,

    // ── Thermal printer (mirrors VoucherPrintState) ───────────────────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    /** Print the summary automatically once a connection is established. */
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val printerLanguage: PrinterLanguage = PrinterLanguage.ESCPOS,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
) {
    /**
     * The cash the rep is actually holding at the end of the day:
     * cash sales + cash collections − cash refunds. Same definition the cash-flow report uses.
     */
    val cashOnHand: Double get() = cashSalesToday + cashCollectedToday - cashReturnsToday
}
