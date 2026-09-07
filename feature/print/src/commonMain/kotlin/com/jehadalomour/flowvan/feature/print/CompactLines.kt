package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine

/**
 * Which print-only line merge the rep chose. NONE prints every line; UNITS folds
 * several lines of ONE item sold on the same unit/price; ALTERNATIVES also folds
 * DIFFERENT items — but only ones the ERP declares substitutes for each other.
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
 *  - ALTERNATIVES: DIFFERENT items may also fold, but ONLY when the ERP lists them
 *    as alternatives of one another (Inventory → Item Alternatives, mirrored onto
 *    each product as [Product.altGroup]). Sharing a price is not enough on its own:
 *    a shampoo and a hairbrush at the same price are not one line item, and folding
 *    them printed a receipt the customer could not reconcile against the goods.
 *    An item with no declared alternative merges with nothing but itself.
 * The price/unit/tax fields stay in the key at every level, and quantities are summed
 * rather than recomputed, so a merge never changes a printed figure.
 */
private data class CompactKey(
    val sku: String,
    val conversionQty: Double,
    val unitPrice: Double,
    val discountPct: Double,
    val taxType: String,
    val taxRate: Double,
)

/**
 * [altGroups] maps sku → its ERP alternatives group. Standing in for the sku is what
 * lets two substitutes share a key; the prefix keeps a group whose key happens to equal
 * some other item's sku from colliding with that item.
 */
private fun InvoiceLine.compactKey(
    mergeAlternatives: Boolean,
    altGroups: Map<String, String>,
): CompactKey =
    CompactKey(
        sku = if (mergeAlternatives) altGroups[sku]?.let { "alt:$it" } ?: sku else sku,
        conversionQty = unitConversionQty,
        unitPrice = unitPrice,
        discountPct = discountPct,
        taxType = taxType,
        taxRate = taxRate,
    )

/** How many lines the receipt would LOSE by compacting at [mergeAlternatives]. 0 → nothing to ask. */
fun compactableCount(
    lines: List<InvoiceLine>,
    mergeAlternatives: Boolean = false,
    altGroups: Map<String, String> = emptyMap(),
): Int = lines.size - lines.groupBy { it.compactKey(mergeAlternatives, altGroups) }.size

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
fun compactLines(
    lines: List<InvoiceLine>,
    mergeAlternatives: Boolean = false,
    altGroups: Map<String, String> = emptyMap(),
): List<InvoiceLine> =
    lines.groupBy { it.compactKey(mergeAlternatives, altGroups) }
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
