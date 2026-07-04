package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.entity.OfferEntity
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferTrigger
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Parses the EXACT cached JSON the device stores for the two live offers, to prove the offline
 * offers cache is readable (the on-device evaluator sees nothing if this returns null).
 */
class OfferDefinitionParserTest {
    // Same config the DI provides for the offer cache.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private fun entity(type: String, trigger: String, reward: String) = OfferEntity(
        id = "id", name = "n", description = null, type = type,
        triggerJson = trigger, rewardJson = reward, eligibilityJson = """{"customerScope":"ALL"}""",
        validFrom = null, validTo = null, daysOfWeekCsv = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 10, stackable = false,
        isActive = true, redemptionCount = 5, createdAt = "2026-06-29T01:21:38.893Z", cachedAt = 0L,
    )

    @Test
    fun parsesPaymentMethodDiscount() {
        val d = entity(
            "PAYMENT_METHOD_DISCOUNT",
            """{"minOrderTotal":10000,"paymentCondition":"CASH"}""",
            """{"kind":"LINE_PERCENT_DISCOUNT","mode":"STATIC","basePercent":5}""",
        ).toDefinition(json)
        assertNotNull(d, "payment offer parsed to null — offline cache unreadable")
        val t = d.trigger as OfferTrigger.PaymentMethod
        assertEquals("CASH", t.paymentCondition)
        assertEquals(10000L, t.minOrderTotalFils)
        val r = d.reward as OfferReward.LinePercent
        assertEquals(5.0, r.basePercent)
        assertTrue(!r.dynamic)
    }

    @Test
    fun parsesItemQtyGift() {
        val d = entity(
            "ITEM_QTY_REWARD",
            """{"itemNumbers":["WATER-1.5L"]}""",
            """{"kind":"GIFT","giftItems":["WATER-1.5L"],"giftsPerStep":3,"itemsPerGift":10}""",
        ).toDefinition(json)
        assertNotNull(d, "gift offer parsed to null — offline cache unreadable")
        val t = d.trigger as OfferTrigger.ItemSet
        assertEquals(listOf("WATER-1.5L"), t.itemNumbers)
        val r = d.reward as OfferReward.Gift
        assertEquals(10, r.itemsPerGift)
        assertEquals(3, r.giftsPerStep)
    }

    @Test
    fun parsesItemAmountDiscount() {
        val d = entity(
            "ITEM_QTY_REWARD",
            """{"itemNumbers":["COLA-330"]}""",
            """{"kind":"ITEM_AMOUNT_DISCOUNT","minQty":12,"baseAmountFils":200,"mode":"STATIC"}""",
        ).toDefinition(json)
        assertNotNull(d, "amount-off offer parsed to null — offline cache unreadable")
        val t = d.trigger as OfferTrigger.ItemSet
        assertEquals(listOf("COLA-330"), t.itemNumbers)
        val r = d.reward as OfferReward.ItemAmount
        assertEquals(12, r.minQty)
        assertEquals(200.0, r.baseAmountFils)
        assertTrue(!r.dynamic)
    }
}
