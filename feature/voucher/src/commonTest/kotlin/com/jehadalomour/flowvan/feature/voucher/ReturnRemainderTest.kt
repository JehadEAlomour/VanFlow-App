package com.jehadalomour.flowvan.feature.voucher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What is still returnable on a sale = what it sold, minus what has come back.
 *
 * The arithmetic the pre-fill and the "fully returned" label both rest on. Kept as
 * a plain map exercise so the rules are readable: a piece returns once, a partly
 * returned line offers only the rest, and a sale with nothing left is spent.
 */
class ReturnRemainderTest {

    private fun remaining(
        sold: Map<String, Double>,
        taken: Map<String, Double>,
    ): Map<String, Double> =
        sold.mapValues { (k, qty) -> qty - (taken[k] ?: 0.0) }.filterValues { it > 0.0001 }

    private val gel = lineKey("p-1", "")
    private val soap = lineKey("p-2", "")
    private val gift = lineKey("p-1", GIFT_UNIT_ID)

    @Test
    fun `nothing returned yet leaves the whole sale`() {
        val left = remaining(mapOf(gel to 12.0, soap to 3.0), emptyMap())
        assertEquals(mapOf(gel to 12.0, soap to 3.0), left)
    }

    @Test
    fun `a partly returned line offers only the rest`() {
        val left = remaining(mapOf(gel to 12.0), mapOf(gel to 5.0))
        assertEquals(7.0, left[gel])
    }

    @Test
    fun `a fully returned line is dropped, not offered at zero`() {
        val left = remaining(mapOf(gel to 12.0, soap to 3.0), mapOf(gel to 12.0))
        assertEquals(setOf(soap), left.keys)
    }

    @Test
    fun `a sale with everything back is spent`() {
        val left = remaining(mapOf(gel to 12.0, gift to 1.0), mapOf(gel to 12.0, gift to 1.0))
        assertTrue(left.isEmpty(), "an empty remainder is what marks the sale fully returned")
    }

    @Test
    fun `the gift is counted apart from the line that earned it`() {
        // 12 paid pieces returned; the free one is still with the customer.
        val left = remaining(mapOf(gel to 12.0, gift to 1.0), mapOf(gel to 12.0))
        assertEquals(mapOf(gift to 1.0), left)
    }
}
