package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceAppliedOffer
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.VoucherTemplate
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
    /** Gift/free items (ITEM_QTY_REWARD picks) resolved for display; unitPrice/lineTotal = 0. */
    val freeLines: List<InvoiceLine> = emptyList(),
    /**
     * Offers applied at sale time (name + discount value in JOD), frozen on the invoice. Drives
     * the itemized per-offer rows + total in the printed footer. Empty → generic discount row.
     */
    val appliedOffers: List<InvoiceAppliedOffer> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    /** Frozen at sale time — drives the TAX-EXEMPT stamp on the receipt. */
    val isTaxExempt: Boolean = false,
    val taxExemptionNumber: String? = null,
    val branch: String = "",
    // Company header — server-first (GET /company-info) when online, else DB cache.
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    /** Company logo (data:...;base64 URI) cached from /company-info; blank → bundled default. */
    val companyLogo: String = "",
    /** Tax-QR payload. Null → no QR available, so the QR block is omitted entirely. */
    val qrData: String? = null,
    /** Render configuration. Jordan defaults today; server-fed in the next step. */
    val template: VoucherTemplate = VoucherTemplate(),

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
