package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

/**
 * One movement on the printed statement.
 *
 * Unlike the on-screen statement — newest first, because that is what a rep
 * scrolling a phone wants — the printed one runs OLDEST FIRST and carries a
 * running [balance]. A statement handed to a shopkeeper is read as an argument:
 * "you owed this, then this happened, so now you owe that". Reversed, with no
 * running total, it is just a list and the closing figure has to be taken on
 * trust.
 */
data class StatementRow(
    val createdAt: Long,
    val number: String,
    /** SALE / RETURN / REQUEST for a voucher, PAYMENT for a collection. */
    val docType: String,
    /** CASH / CHEQUE / TRANSFER — only set when [docType] is PAYMENT. */
    val method: String? = null,
    /** Increases what the customer owes. Zero on a credit row. */
    val debit: Double = 0.0,
    /** Reduces what the customer owes. Zero on a debit row. */
    val credit: Double = 0.0,
    /** Balance AFTER this row. */
    val balance: Double = 0.0,
)

data class StatementPrintState(
    val isLoading: Boolean = true,
    val customerId: String = "",
    val customerNameAr: String = "",
    val customerCode: String = "",
    val customerPhone: String = "",
    val salesmanNameAr: String = "",
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    /**
     * What the customer owed the moment the period started, from every movement
     * BEFORE it. Without this the closing figure is only the period's net, which
     * is not what "كشف حساب" means to the person being handed it.
     */
    val openingBalance: Double = 0.0,
    val rows: List<StatementRow> = emptyList(),
    /**
     * The figures came off THIS DEVICE, not the server — so they cover only what
     * this handset created and may be short of another van's invoices. The screen
     * says so before the rep prints; see AccountStatementScreen.
     */
    val isLocalOnly: Boolean = false,
    /** When the paper was produced — a statement without this is undatable. */
    val printedAt: Long = 0L,

    // Company header — server-first (GET /company-info) when online, else DB cache.
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    /** Company logo (data:...;base64 URI); blank → the bundled default mark. */
    val companyLogo: String = "",

    // ── Thermal printer ──────────────────────────────────────────────────────
    val printerState: PrinterState = PrinterState.Disconnected,
    val isPrinting: Boolean = false,
    val printMessageAr: String? = null,
    val showConnectDialog: Boolean = false,
    /** Print automatically once a connection is established from the dialog. */
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
) {
    val totalDebits: Double get() = rows.sumOf { it.debit }
    val totalCredits: Double get() = rows.sumOf { it.credit }

    /** What the customer owes at the end of the period. */
    val closingBalance: Double get() = openingBalance + totalDebits - totalCredits
}

sealed interface StatementPrintEvent {
    /** Tapped "thermal print" while disconnected — open the dialog, print once connected. */
    data object RequestConnectThenPrint : StatementPrintEvent
    data object DismissConnectDialog : StatementPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : StatementPrintEvent
    data class ConnectAddressChanged(val address: String) : StatementPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : StatementPrintEvent
    data object RefreshDevices : StatementPrintEvent
    data object Connect : StatementPrintEvent
    data object Disconnect : StatementPrintEvent

    /** UI captured the statement as PNG bytes; send it to the printer. */
    data class Print(val statementPng: ByteArray) : StatementPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && statementPng.contentEquals(other.statementPng))
        override fun hashCode() = statementPng.contentHashCode()
    }

    data object DismissMessage : StatementPrintEvent
}
