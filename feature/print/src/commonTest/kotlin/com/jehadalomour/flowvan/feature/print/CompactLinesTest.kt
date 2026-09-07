package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The print-only line merge, and the rule that decides which DIFFERENT items may fold.
 *
 * The bug this pins down: "merge alternatives" used to fold ANY two items that happened
 * to share a price and unit, so a shampoo and a hairbrush both at 5.000 printed as one
 * row the customer could not reconcile against the goods in the bag. Only items the ERP
 * lists as alternatives of each other (products.altGroup) may fold now.
 */
class CompactLinesTest {

    private fun line(sku: String, qty: Double, price: Double = 5.0, unit: String = "حبة") =
        InvoiceLine(
            productId = "p-$sku",
            sku = sku,
            nameAr = "صنف $sku",
            qty = qty,
            unitPrice = price,
            lineTotal = qty * price,
            unit = unit,
            taxAmount = qty * price * 0.16,
            taxRate = 0.16,
        )

    /** 130/131/132 are one ERP alternatives group; 900 is in none. */
    private val groups = mapOf("130" to "130", "131" to "130", "132" to "130")

    @Test
    fun `same-priced items that are NOT alternatives stay apart`() {
        val lines = listOf(line("130", 2.0), line("900", 3.0))
        assertEquals(0, compactableCount(lines, mergeAlternatives = true, altGroups = groups))
        assertEquals(2, compactLines(lines, mergeAlternatives = true, altGroups = groups).size)
    }

    @Test
    fun `items the ERP declares alternatives fold into one row`() {
        val lines = listOf(line("130", 2.0), line("131", 3.0), line("132", 1.0))
        assertEquals(2, compactableCount(lines, mergeAlternatives = true, altGroups = groups))
        val merged = compactLines(lines, mergeAlternatives = true, altGroups = groups)
        assertEquals(1, merged.size)
        assertEquals(6.0, merged.single().qty)
        // Summed, never recomputed — the printed column foots to the un-merged figure.
        assertEquals(lines.sumOf { it.lineTotal }, merged.single().lineTotal)
        assertEquals(lines.sumOf { it.taxAmount }, merged.single().taxAmount)
        // The row takes the first member's identity.
        assertEquals("130", merged.single().sku)
    }

    @Test
    fun `alternatives at different prices never fold`() {
        val lines = listOf(line("130", 2.0, price = 5.0), line("131", 3.0, price = 6.0))
        assertEquals(0, compactableCount(lines, mergeAlternatives = true, altGroups = groups))
    }

    @Test
    fun `with no groups synced the alternatives level folds nothing extra`() {
        val lines = listOf(line("130", 2.0), line("131", 3.0))
        assertEquals(0, compactableCount(lines, mergeAlternatives = true, altGroups = emptyMap()))
    }

    @Test
    fun `the unit level still folds two lines of one item`() {
        val lines = listOf(line("130", 2.0), line("130", 3.0))
        assertEquals(1, compactableCount(lines, mergeAlternatives = false))
        assertEquals(5.0, compactLines(lines, mergeAlternatives = false).single().qty)
    }

    @Test
    fun `merged members disagreeing on unit print a blank unit`() {
        val lines = listOf(line("130", 2.0, unit = "حبة"), line("131", 1.0, unit = "علبة"))
        val merged = compactLines(lines, mergeAlternatives = true, altGroups = groups)
        assertEquals(1, merged.size)
        assertTrue(merged.single().unit.isEmpty())
    }
}
