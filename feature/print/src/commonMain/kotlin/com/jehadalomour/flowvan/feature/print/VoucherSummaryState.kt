package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.model.PaymentType

/** One line on the printed voucher summary. */
data class VoucherSummaryRow(
    val id: String,
    val number: String,
    val customerName: String,
    /** Raw stored type: "SALE" or "RETURN". */
    val type: String,
    val paymentType: PaymentType,
    val total: Double,
    val createdAt: Long,
)

data class VoucherSummaryState(
    // ── Date range (inclusive) ────────────────────────────────────────────────
    val from: Long = 0L,
    val to: Long = 0L,

    // ── Rows + footer totals ──────────────────────────────────────────────────
    val rows: List<VoucherSummaryRow> = emptyList(),
    val totalSales: Double = 0.0,
    val totalReturns: Double = 0.0,
    val cashSales: Double = 0.0,
    val cashReturns: Double = 0.0,
    val creditSales: Double = 0.0,
    val creditReturns: Double = 0.0,

    // ── Receipt header ────────────────────────────────────────────────────────
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    /** Company logo (data:...;base64 URI) cached from /company-info; blank → bundled default. */
    val companyLogo: String = "",
    val branch: String = "",
    val salesmanNameAr: String = "",
    /** Millis timestamp printed on the summary; captured when the screen loads. */
    val reportAt: Long = 0L,

    // ── Thermal printer (mirrors VoucherPrintState / EndOfDayState) ────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    /** Print the summary automatically once a connection is established. */
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
)
