package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine

/**
 * Compacting a receipt: several lines of ONE item, sold in different units, printed as one.
 *
 * With colour units a customer can buy 3 أحمر + 2 أزرق + 1 أخضر of the same product and get
 * six near-identical rows on the receipt. The rep is often asked for one row — "6 حبة" — even
 * though the voucher must keep the variants apart for stock. So this is a PRINT-ONLY view:
 * nothing here touches the saved invoice, the posted voucher or the stock movement.
 *
 * Lines merge only when merging cannot change a number the customer sees: same item, same
 * unit factor, same unit price, same discount and the same tax treatment. Two colours priced
 * differently stay apart — folding them would print a price nobody agreed to.
 */
private data class CompactKey(
    val sku: String,
    val conversionQty: Double,
    val unitPrice: Double,
    val discountPct: Double,
    val taxType: String,
    val taxRate: Double,
)

private val InvoiceLine.compactKey: CompactKey
    get() = CompactKey(sku, unitConversionQty, unitPrice, discountPct, taxType, taxRate)

/** How many lines the receipt would LOSE by compacting. 0 → nothing to ask the user about. */
fun compactableCount(lines: List<InvoiceLine>): Int =
    lines.size - lines.groupBy { it.compactKey }.size

/**
 * The compacted lines, in the order the first member of each group appeared.
 *
 * Quantities and totals are summed rather than recomputed from a unit price, so the printed
 * column still foots to exactly the same figure as the un-compacted receipt.
 *
 * The unit cell keeps the shared unit name when every merged line agrees, and goes blank when
 * they do not — a row that is "3 red + 2 blue" is honestly not any one unit, and the print
 * grid already renders a blank unit as "—".
 */
fun compactLines(lines: List<InvoiceLine>): List<InvoiceLine> =
    lines.groupBy { it.compactKey }
        .values
        .map { group ->
            if (group.size == 1) group.first()
            else group.first().copy(
                qty = group.sumOf { it.qty },
                lineTotal = group.sumOf { it.lineTotal },
                taxAmount = group.sumOf { it.taxAmount },
                unit = group.map { it.unit }.distinct().singleOrNull() ?: "",
                unitId = "",
            )
        }
