package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine

/**
 * Which print-only line merge the rep chose. NONE prints every line; UNITS folds
 * several lines of ONE item sold on the same unit/price; ALTERNATIVES also folds
 * DIFFERENT items that share the same unit + price (same-priced substitutes).
 */
enum class CompactMode { NONE, UNITS, ALTERNATIVES }

/**
 * Compacting a receipt: several lines printed as one.
 *
 * With colour units a customer can buy 3 أحمر + 2 أزرق + 1 أخضر of the same product and get
 * six near-identical rows on the receipt. The rep is often asked for one row — "6 حبة" — even
 * though the voucher must keep the variants apart for stock. So this is a PRINT-ONLY view:
 * nothing here touches the saved invoice, the posted voucher or the stock movement.
 *
 * Two levels, both gated so merging can never change a number the customer sees:
 *  - UNITS: same item, same unit factor, same unit price, discount and tax.
 *  - ALTERNATIVES: same unit/price/discount/tax but DIFFERENT items — same-priced
 *    substitutes fold into one row. The price match is the whole safety: the summed
 *    total foots to exactly the un-merged figure, so nothing is misstated. The row
 *    takes the first member's name (the rest are its same-priced alternatives).
 * Anything priced differently always stays apart — folding it would print a price
 * nobody agreed to.
 */
private data class CompactKey(
    val sku: String,
    val conversionQty: Double,
    val unitPrice: Double,
    val discountPct: Double,
    val taxType: String,
    val taxRate: Double,
)

private fun InvoiceLine.compactKey(mergeAlternatives: Boolean): CompactKey =
    CompactKey(
        // Dropping the sku is what lets same-priced DIFFERENT items merge; the price
        // and tax fields stay in the key, so a merge never changes a printed figure.
        sku = if (mergeAlternatives) "" else sku,
        conversionQty = unitConversionQty,
        unitPrice = unitPrice,
        discountPct = discountPct,
        taxType = taxType,
        taxRate = taxRate,
    )

/** How many lines the receipt would LOSE by compacting at [mergeAlternatives]. 0 → nothing to ask. */
fun compactableCount(lines: List<InvoiceLine>, mergeAlternatives: Boolean = false): Int =
    lines.size - lines.groupBy { it.compactKey(mergeAlternatives) }.size

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
fun compactLines(lines: List<InvoiceLine>, mergeAlternatives: Boolean = false): List<InvoiceLine> =
    lines.groupBy { it.compactKey(mergeAlternatives) }
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
