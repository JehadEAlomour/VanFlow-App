package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.CartLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A return's cart rows are keyed for a LazyColumn, and a duplicate key is a crash,
 * not a glitch: "Key ... was already used".
 *
 * The case that crashed: buy 12 of an item, get 1 of the SAME item free. The paid
 * line and the gift are one product in one base unit, so both keyed on
 * (productId, "").
 */
class GiftLineKeyTest {

    private fun line(productId: String, unitId: String) = CartLine(
        productId = productId,
        sku = "130",
        nameAr = "جل شعر",
        unitPrice = 1.0,
        qty = 1.0,
        unitId = unitId,
    )

    @Test
    fun `a gift never collides with the paid line that earned it`() {
        val paid = line("p-1", "")
        val gift = line("p-1", GIFT_UNIT_ID)
        assertNotEquals(paid.key, gift.key)
        assertEquals(2, listOf(paid, gift).map { it.key }.distinct().size)
    }

    @Test
    fun `every row in a mixed return cart has its own key`() {
        val cart = listOf(
            line("p-1", ""),                 // paid, base unit
            line("p-1", "u-carton"),         // paid, a packaging unit of the same item
            line("p-1", GIFT_UNIT_ID),       // the gift of that same item
            line("p-2", ""),                 // another item
        )
        assertEquals(cart.size, cart.map { it.key }.distinct().size)
    }
}
