package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the van's printed stock sheet is allowed to say.
 *
 * The sheet is a count: a rep walks the van against it and a supervisor signs
 * it. So the three things tested here are the three that make it useless if
 * they are wrong — an item that is not on the van appearing on it, the order
 * changing between two prints of the same van, and a line's value not being
 * the quantity times the price the screen shows.
 */
class VanStockPrintRowsTest {

    private fun product(
        sku: String,
        nameAr: String = "",
        nameEn: String = "",
        vanStock: Int = 0,
        salePrice: Double = 1.0,
        unit: String = "PCS",
    ) = Product(
        id = sku,
        sku = sku,
        nameAr = nameAr,
        nameEn = nameEn,
        category = "",
        unit = unit,
        salePrice = salePrice,
        costPrice = 0.0,
        vanStock = vanStock,
        minStock = 0,
        expiryDate = null,
        brand = null,
    )

    @Test
    fun `only items the van is actually carrying are printed`() {
        val rows = vanStockRows(
            listOf(
                product("A", nameAr = "أ", vanStock = 3),
                product("B", nameAr = "ب", vanStock = 0),
                // A negative on-hand is a data fault, not a thing in the van —
                // and a line saying minus two is not something anyone can count.
                product("C", nameAr = "ج", vanStock = -2),
            ),
        )
        assertEquals(listOf("A"), rows.map { it.itemNumber })
    }

    @Test
    fun `rows come out in name order so two counts can be compared`() {
        val rows = vanStockRows(
            listOf(
                product("A", nameAr = "جبنة", vanStock = 1),
                product("B", nameAr = "ألبان", vanStock = 1),
                product("C", nameAr = "بسكويت", vanStock = 1),
            ),
        )
        assertEquals(listOf("ألبان", "بسكويت", "جبنة"), rows.map { it.name })
    }

    @Test
    fun `an item with no Arabic name still gets named`() {
        // Blank, not missing: the catalogue carries both names and either may be
        // empty. A blank line on a count sheet is an item nobody can find.
        val rows = vanStockRows(listOf(product("A", nameEn = "Water 1L", vanStock = 1)))
        assertEquals("Water 1L", rows.single().name)
    }

    @Test
    fun `a line is worth its quantity times the sale price`() {
        val rows = vanStockRows(listOf(product("A", nameAr = "أ", vanStock = 4, salePrice = 2.25)))
        assertEquals(9.0, rows.single().value)
    }

    @Test
    fun `the totals on the sheet add up the lines printed on it`() {
        val state = VanStockPrintState(
            rows = vanStockRows(
                listOf(
                    product("A", nameAr = "أ", vanStock = 4, salePrice = 2.25),
                    product("B", nameAr = "ب", vanStock = 6, salePrice = 0.5),
                    product("C", nameAr = "ج", vanStock = 0, salePrice = 100.0),
                ),
            ),
        )
        // Lines and pieces are different questions, and the sheet states both.
        assertEquals(2, state.itemCount)
        assertEquals(10, state.totalQty)
        assertEquals(12.0, state.totalValue)
        // The item at zero paid nothing into the total it is not printed in.
        assertTrue(state.rows.none { it.itemNumber == "C" })
    }

    @Test
    fun `the unit travels with the line`() {
        // It is printed beside the code: "6" of something is a count only once
        // you know whether it means boxes or bottles.
        val rows = vanStockRows(listOf(product("A", nameAr = "أ", vanStock = 6, unit = "CTN")))
        assertEquals("CTN", rows.single().unit)
    }
}
