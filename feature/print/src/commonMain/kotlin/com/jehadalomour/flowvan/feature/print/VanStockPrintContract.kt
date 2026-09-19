package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType

/** One item on the van, as the printed sheet needs it. */
data class VanStockPrintRow(
    val itemNumber: String,
    val name: String,
    /** Pieces on the van. Only positive rows are printed — see the view model. */
    val qty: Int,
    val unit: String,
    /** Pieces × sale price, in JOD. */
    val value: Double,
)

data class VanStockPrintState(
    val isLoading: Boolean = true,
    val rows: List<VanStockPrintRow> = emptyList(),
    val salesmanNameAr: String = "",
    /** When the paper was produced — a stock sheet without this is undatable. */
    val printedAt: Long = 0L,

    // Company header — the locally cached profile, like every other receipt.
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
    val pendingPrint: Boolean = false,
    val connectType: PrinterType = PrinterType.BLUETOOTH,
    /**
     * ESC/POS or Zebra CPCL. Device-wide and persisted on the printer, so the
     * choice made on any print screen holds for all of them.
     */
    val connectLanguage: PrinterLanguage = PrinterLanguage.ESCPOS,
    val connectAddress: String = "",
    val discoveredDevices: List<PrinterTarget> = emptyList(),
) {
    /** Lines on the sheet — how many DIFFERENT items the van carries. */
    val itemCount: Int get() = rows.size

    /**
     * Every piece on the van added up.
     *
     * The figure a salesman counts against, as distinct from the number of
     * lines: a van with 3 items and 300 pieces is not a van with 3 of anything.
     */
    val totalQty: Int get() = rows.sumOf { it.qty }

    val totalValue: Double get() = rows.sumOf { it.value }
}

sealed interface VanStockPrintEvent {
    data object RequestConnectThenPrint : VanStockPrintEvent
    data object DismissConnectDialog : VanStockPrintEvent
    data class ConnectTypeSelected(val type: PrinterType) : VanStockPrintEvent
    data class PrinterLanguageSelected(val language: PrinterLanguage) : VanStockPrintEvent
    data class ConnectAddressChanged(val address: String) : VanStockPrintEvent
    data class DeviceSelected(val target: PrinterTarget) : VanStockPrintEvent
    data object RefreshDevices : VanStockPrintEvent
    data object Connect : VanStockPrintEvent
    data object Disconnect : VanStockPrintEvent

    /** UI captured the sheet as PNG bytes; send it to the printer. */
    data class Print(val png: ByteArray) : VanStockPrintEvent {
        override fun equals(other: Any?) =
            this === other || (other is Print && png.contentEquals(other.png))
        override fun hashCode() = png.contentHashCode()
    }

    data object DismissMessage : VanStockPrintEvent
}

/**
 * The catalogue as this sheet needs it.
 *
 * Pure, and separate from the view model, because everything that can actually
 * be wrong on this paper is here: which items appear, in what order, and what
 * each line is worth.
 *
 * Only what is ON the van. The catalogue holds every item the company sells;
 * printing the ones at zero would bury the twenty lines a rep has to count
 * under hundreds he does not, and spend the roll doing it. Sorted by the name
 * the line is printed under, because the sheet is read against shelves and a
 * stable order is what lets a second count be compared with the first.
 */
fun vanStockRows(products: List<Product>): List<VanStockPrintRow> =
    products
        .filter { it.vanStock > 0 }
        .map { p ->
            VanStockPrintRow(
                itemNumber = p.sku,
                name = p.nameAr.ifBlank { p.nameEn },
                qty = p.vanStock,
                unit = p.unit,
                // Sale price, matching the van-stock screen's own inventory
                // value — the two are read side by side and may not disagree.
                value = p.vanStock * p.salePrice,
            )
        }
        .sortedBy { it.name }
