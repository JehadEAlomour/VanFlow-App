package com.jehadalomour.flowvan.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Does a gift line actually survive a JSON round-trip? */
class FreeLineSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `free lines round-trip`() {
        val original = listOf(FreeLine(itemNumber = "130", qty = 1.0, unitPriceJod = 1.333, offerId = "o1"))
        val text = json.encodeToString(original)
        val back = json.decodeFromString<List<FreeLine>>(text)
        assertEquals(original, back)
    }
}
