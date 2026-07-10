package com.jehadalomour.flowvan.core.domain.offers

import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.model.OfferEligibilityRule
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferTrigger
import com.jehadalomour.flowvan.core.model.OfferType
import com.jehadalomour.flowvan.core.network.dto.EvaluationResultDto
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parity tests for [LocalOfferEvaluator] — ported from the backend `offers-engine.spec.ts`
 * (cash-van, deploy/render). Items: A = 1000 fils, B = 500 fils, C = 750 fils, all tax-free.
 */
class LocalOfferEvaluatorTest {

    private val items = mapOf(
        "A" to LocalOfferEvaluator.ItemInfo(1000, 0.0),
        "B" to LocalOfferEvaluator.ItemInfo(500, 0.0),
        "C" to LocalOfferEvaluator.ItemInfo(750, 0.0),
    )

    private fun ctx(payment: String? = null, chosen: List<String> = emptyList()) =
        LocalOfferEvaluator.Context(
            customerNumber = null, customerCategory = null, customerRegionId = null,
            customerRepId = null, repId = null, storeNumber = null,
            paymentMethod = payment, chosenFreeItems = chosen, isNewCustomer = false,
            at = LocalDateTime(2026, 1, 1, 12, 0), nowMs = 0L,
        )

    private val allEligibility = OfferEligibilityRule("ALL", null, null, null, null, null)

    private fun payOffer(
        id: String = "pay",
        condition: String = "CASH",
        base: Double,
        dynamic: Boolean = false,
        multiplier: Double? = null,
        itemsPerStep: Int? = null,
        maxPercent: Double? = null,
        minOrderTotalFils: Long? = null,
        minItemCount: Int? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.PAYMENT_METHOD_DISCOUNT,
        trigger = OfferTrigger.PaymentMethod(condition, minOrderTotalFils, minItemCount),
        reward = OfferReward.LinePercent(base, dynamic, multiplier, itemsPerStep, maxPercent),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun payAmtOffer(
        id: String = "payamt",
        condition: String = "CASH",
        baseAmountFils: Double,
        dynamic: Boolean = false,
        multiplier: Double? = null,
        itemsPerStep: Int? = null,
        maxAmountFils: Double? = null,
        maxPercentOfPrice: Double? = null,
        minItemCount: Int? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.PAYMENT_METHOD_DISCOUNT,
        trigger = OfferTrigger.PaymentMethod(condition, null, minItemCount),
        reward = OfferReward.LineAmount(baseAmountFils, dynamic, multiplier, itemsPerStep, maxAmountFils, maxPercentOfPrice),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun giftOffer(
        id: String = "gift",
        itemNumbers: List<String>,
        giftItems: List<String>,
        itemsPerGift: Int,
        giftsPerStep: Int = 1,
        maxFreeQty: Int? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.ITEM_QTY_REWARD,
        trigger = OfferTrigger.ItemSet(itemNumbers),
        reward = OfferReward.Gift(giftItems, itemsPerGift, giftsPerStep, maxFreeQty),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun itemPctOffer(
        id: String = "itempct",
        itemNumbers: List<String>,
        minQty: Int,
        base: Double,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.ITEM_QTY_REWARD,
        trigger = OfferTrigger.ItemSet(itemNumbers),
        reward = OfferReward.ItemPercent(minQty, base, dynamic = false, multiplier = null, itemsPerStep = null, maxPercent = null),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun itemAmtOffer(
        id: String = "itemamt",
        itemNumbers: List<String>,
        minQty: Int,
        baseAmountFils: Double,
        dynamic: Boolean = false,
        multiplier: Double? = null,
        itemsPerStep: Int? = null,
        maxAmountFils: Double? = null,
        maxPercentOfPrice: Double? = null,
        paymentCondition: String? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.ITEM_QTY_REWARD,
        trigger = OfferTrigger.ItemSet(itemNumbers, paymentCondition),
        reward = OfferReward.ItemAmount(minQty, baseAmountFils, dynamic, multiplier, itemsPerStep, maxAmountFils, maxPercentOfPrice),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun EvaluationResultDto.disc(item: String) = lines.first { it.itemNumber == item }.lineDiscountFils

    // ── PAYMENT_METHOD_DISCOUNT ──────────────────────────────────────────────

    @Test
    fun staticPercentAppliesToEveryLineForCash() {
        val r = LocalOfferEvaluator.evaluate(
            listOf("A" to 4.0, "B" to 2.0), listOf(payOffer(base = 5.0)), items, ctx("CASH"),
        )
        assertEquals(200, r.disc("A")) // 4000 × 5%
        assertEquals(50, r.disc("B"))  // 1000 × 5%
        assertEquals(250, r.totals.lineDiscountFils)
        assertEquals(1, r.appliedOffers.size)
    }

    @Test
    fun nonCreditTreatedAsCashButNotCredit() {
        val offers = listOf(payOffer(base = 5.0))
        assertEquals(1, LocalOfferEvaluator.evaluate(listOf("A" to 4.0), offers, items, ctx("CHEQUE")).appliedOffers.size)
        assertEquals(0, LocalOfferEvaluator.evaluate(listOf("A" to 4.0), offers, items, ctx("CREDIT")).appliedOffers.size)
    }

    @Test
    fun creditOfferOnlyOnCredit() {
        val offers = listOf(payOffer(condition = "CREDIT", base = 10.0))
        assertEquals(0, LocalOfferEvaluator.evaluate(listOf("A" to 1.0), offers, items, ctx("CASH")).appliedOffers.size)
        assertEquals(1, LocalOfferEvaluator.evaluate(listOf("A" to 1.0), offers, items, ctx("CREDIT")).appliedOffers.size)
    }

    @Test
    fun respectsMinOrderTotalAndMinItemCount() {
        val offers = listOf(payOffer(base = 5.0, minOrderTotalFils = 6000, minItemCount = 5))
        assertEquals(0, LocalOfferEvaluator.evaluate(listOf("A" to 4.0), offers, items, ctx("CASH")).appliedOffers.size)
        assertEquals(1, LocalOfferEvaluator.evaluate(listOf("A" to 6.0), offers, items, ctx("CASH")).appliedOffers.size)
    }

    @Test
    fun dynamicStepsUpAndCaps() {
        val offers = listOf(payOffer(base = 10.0, dynamic = true, multiplier = 0.5, itemsPerStep = 6, maxPercent = 25.0))
        assertEquals(500, LocalOfferEvaluator.evaluate(listOf("A" to 5.0), offers, items, ctx("CASH")).disc("A"))
        assertEquals(900, LocalOfferEvaluator.evaluate(listOf("A" to 6.0), offers, items, ctx("CASH")).disc("A"))
        assertEquals(7500, LocalOfferEvaluator.evaluate(listOf("A" to 30.0), offers, items, ctx("CASH")).disc("A"))
    }

    @Test
    fun dynamicAnchorsBaseAtMinItemCount() {
        val offers = listOf(payOffer(base = 3.0, dynamic = true, multiplier = 1.0, itemsPerStep = 10, minItemCount = 10))
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), offers, items, ctx("CASH")).lines.firstOrNull()?.lineDiscountFils ?: 0
        assertEquals(300, d(10.0))
        assertEquals(570, d(19.0))
        assertEquals(1200, d(20.0))
        assertEquals(2700, d(30.0))
    }

    // ── PAYMENT_METHOD_DISCOUNT (amount) ─────────────────────────────────────

    @Test
    fun lineAmountTakesFixedOffEachUnitForCash() {
        val r = LocalOfferEvaluator.evaluate(
            listOf("A" to 4.0, "B" to 2.0), listOf(payAmtOffer(baseAmountFils = 300.0)), items, ctx("CASH"),
        )
        assertEquals(1200, r.disc("A")) // 300 × 4
        assertEquals(600, r.disc("B"))  // 300 × 2
        assertEquals(1800, r.totals.lineDiscountFils)
    }

    @Test
    fun lineAmountClampsToLineGross() {
        val r = LocalOfferEvaluator.evaluate(
            listOf("A" to 1.0, "B" to 1.0), listOf(payAmtOffer(baseAmountFils = 2000.0)), items, ctx("CASH"),
        )
        assertEquals(1000, r.disc("A"))
        assertEquals(500, r.disc("B"))
    }

    @Test
    fun lineAmountDynamicStepsAndCaps() {
        val offers = listOf(payAmtOffer(baseAmountFils = 100.0, dynamic = true, multiplier = 0.5, itemsPerStep = 6, maxAmountFils = 250.0, minItemCount = 6))
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), offers, items, ctx("CASH")).disc("A")
        assertEquals(600, d(6.0))     // per-unit 100 × 6
        assertEquals(1800, d(12.0))   // 1 step → per-unit 150 × 12
        assertEquals(15000, d(60.0))  // per-unit capped 250 × 60
    }

    @Test
    fun lineAmountCapsAtMaxPercentOfPrice() {
        // Base 500/unit capped per line at 20% of unit price: A(1000)→200, B(500)→100.
        val offers = listOf(payAmtOffer(baseAmountFils = 500.0, maxPercentOfPrice = 20.0))
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 3.0, "B" to 2.0), offers, items, ctx("CASH"))
        assertEquals(600, r.disc("A")) // 200 × 3
        assertEquals(200, r.disc("B")) // 100 × 2
    }

    @Test
    fun perLineHigherFilsWinsBetweenPercentAndAmountPayment() {
        val offers = listOf(
            payOffer(id = "pct10", base = 10.0),
            payAmtOffer(id = "amt80", baseAmountFils = 80.0),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 4.0, "B" to 1.0), offers, items, ctx("CASH"))
        assertEquals(400, r.disc("A")) // 10% of 4000 beats amount 80 × 4 = 320
        assertEquals(80, r.disc("B"))  // amount 80 × 1 beats 10% of 500 = 50
        assertEquals(listOf("amt80", "pct10"), r.appliedOffers.map { it.offerId }.sorted())
    }

    // ── ITEM_QTY_REWARD (gift) ───────────────────────────────────────────────

    @Test
    fun giftSurfacesChoiceThenResolvesPicksToFreeLines() {
        val offer = giftOffer(itemNumbers = listOf("A"), giftItems = listOf("B"), itemsPerGift = 10)
        val choiceOnly = LocalOfferEvaluator.evaluate(listOf("A" to 22.0), listOf(offer), items, ctx())
        assertEquals(listOf("B"), choiceOnly.appliedOffers[0].freeItemChoice?.choices)
        assertEquals(2, choiceOnly.appliedOffers[0].freeItemChoice?.qty)
        assertTrue(choiceOnly.freeLines.isEmpty())

        val picked = LocalOfferEvaluator.evaluate(listOf("A" to 22.0), listOf(offer), items, ctx(chosen = listOf("B", "B", "B")))
        assertEquals(2, picked.freeLines.size)
        assertTrue(picked.freeLines.all { it.itemNumber == "B" && it.qty == 1.0 && it.unitPriceFils == 500L })
    }

    @Test
    fun giftIsOnePerItemsPerGiftCappedNoneBelowFirst() {
        val offer = giftOffer(itemNumbers = listOf("A"), giftItems = listOf("B"), itemsPerGift = 10, maxFreeQty = 5)
        fun free(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).appliedOffers.firstOrNull()?.freeItemChoice?.qty
        assertEquals(1, free(14.0))
        assertEquals(2, free(20.0))
        assertEquals(5, free(1000.0))
        assertTrue(LocalOfferEvaluator.evaluate(listOf("A" to 9.0), listOf(offer), items, ctx()).appliedOffers.isEmpty())
    }

    @Test
    fun giftGrantsGiftsPerStep() {
        val offer = giftOffer(itemNumbers = listOf("A"), giftItems = listOf("B", "C"), itemsPerGift = 10, giftsPerStep = 3)
        fun free(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).appliedOffers.firstOrNull()?.freeItemChoice?.qty
        assertEquals(3, free(10.0))
        assertEquals(6, free(20.0))
        assertNull(free(9.0))
    }

    @Test
    fun giftRespectsMaxFreeQtyWithGiftsPerStep() {
        val offer = giftOffer(itemNumbers = listOf("A"), giftItems = listOf("B"), itemsPerGift = 10, giftsPerStep = 3, maxFreeQty = 3)
        fun free(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).appliedOffers.firstOrNull()?.freeItemChoice?.qty
        assertEquals(3, free(10.0))
        assertEquals(3, free(50.0))
    }

    // ── ITEM_QTY_REWARD (percent) ────────────────────────────────────────────

    @Test
    fun itemPercentAppliesToSelectedItemsOnlyAboveMinQty() {
        val offer = itemPctOffer(itemNumbers = listOf("A"), minQty = 12, base = 10.0)
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 12.0, "B" to 5.0), listOf(offer), items, ctx())
        assertEquals(1200, r.disc("A"))
        assertEquals(0, r.disc("B"))
        assertTrue(LocalOfferEvaluator.evaluate(listOf("A" to 11.0), listOf(offer), items, ctx()).appliedOffers.isEmpty())
    }

    @Test
    fun itemPercentCountsCombinedQty() {
        val offer = itemPctOffer(itemNumbers = listOf("A", "B"), minQty = 10, base = 10.0)
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 6.0, "B" to 5.0), listOf(offer), items, ctx())
        assertEquals(600, r.disc("A"))
        assertEquals(250, r.disc("B"))
    }

    // ── ITEM_QTY_REWARD (amount) ─────────────────────────────────────────────

    @Test
    fun itemAmountTakesFlatAmountOffEachUnitAboveMinQty() {
        val offer = itemAmtOffer(itemNumbers = listOf("A"), minQty = 12, baseAmountFils = 200.0)
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 12.0, "B" to 5.0), listOf(offer), items, ctx())
        assertEquals(2400, r.disc("A")) // 200 × 12
        assertEquals(0, r.disc("B"))
        assertTrue(LocalOfferEvaluator.evaluate(listOf("A" to 11.0), listOf(offer), items, ctx()).appliedOffers.isEmpty())
    }

    @Test
    fun itemOfferPaymentGateOnlyAppliesOnMatchingPayment() {
        val offer = itemAmtOffer(
            itemNumbers = listOf("A"), minQty = 1, baseAmountFils = 100.0, paymentCondition = "CREDIT",
        )
        assertEquals(
            1,
            LocalOfferEvaluator.evaluate(listOf("A" to 2.0), listOf(offer), items, ctx("CREDIT")).appliedOffers.size,
        )
        assertTrue(
            LocalOfferEvaluator.evaluate(listOf("A" to 2.0), listOf(offer), items, ctx("CASH")).appliedOffers.isEmpty(),
        )
    }

    @Test
    fun itemAmountClampsToLineGross() {
        val offer = itemAmtOffer(itemNumbers = listOf("A"), minQty = 1, baseAmountFils = 2000.0)
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 3.0), listOf(offer), items, ctx())
        assertEquals(3000, r.disc("A")) // gross, not 6000
        assertEquals(0, r.lines.first { it.itemNumber == "A" }.lineNetFils)
    }

    @Test
    fun itemAmountDynamicStepsAndCaps() {
        val offer = itemAmtOffer(
            itemNumbers = listOf("A"), minQty = 12, baseAmountFils = 100.0,
            dynamic = true, multiplier = 0.5, itemsPerStep = 6, maxAmountFils = 250.0,
        )
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).disc("A")
        assertEquals(1200, d(12.0)) // 100/unit × 12
        assertEquals(2700, d(18.0)) // 150/unit × 18
        assertEquals(15000, d(60.0)) // capped 250/unit × 60
    }

    @Test
    fun amountBeatsPercentWhenHigherFils() {
        val offers = listOf(
            itemAmtOffer(id = "amt", itemNumbers = listOf("A"), minQty = 1, baseAmountFils = 300.0),
            itemPctOffer(id = "pct", itemNumbers = listOf("A"), minQty = 1, base = 10.0),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 4.0), offers, items, ctx())
        assertEquals(1200, r.disc("A")) // 300 × 4 beats 100 × 4
        assertEquals(listOf("amt"), r.appliedOffers.map { it.offerId })
    }

    // ── conflict resolution (max within, add across) ─────────────────────────

    @Test
    fun twoPaymentOffersKeepOnlyHighest() {
        val offers = listOf(payOffer(id = "big", base = 20.0), payOffer(id = "small", base = 5.0))
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 10.0), offers, items, ctx("CASH"))
        assertEquals(1, r.appliedOffers.size)
        assertEquals("big", r.appliedOffers[0].offerId)
        assertEquals(2000, r.totals.lineDiscountFils)
    }

    @Test
    fun twoItemOffersSameItemKeepOnlyHighest() {
        val offers = listOf(
            itemPctOffer(id = "i8", itemNumbers = listOf("A"), minQty = 1, base = 8.0),
            itemPctOffer(id = "i3", itemNumbers = listOf("A"), minQty = 1, base = 3.0),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 10.0), offers, items, ctx())
        assertEquals(800, r.disc("A"))
        assertEquals(listOf("i8"), r.appliedOffers.map { it.offerId })
    }

    @Test
    fun paymentPlusItemDiscountsAddOnTheSameLine() {
        val offers = listOf(
            payOffer(id = "pay10", base = 10.0),
            itemPctOffer(id = "item5", itemNumbers = listOf("A"), minQty = 1, base = 5.0),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 10.0, "B" to 10.0), offers, items, ctx("CASH"))
        assertEquals(1500, r.disc("A")) // 15% of 10000
        assertEquals(500, r.disc("B"))  // 10% of 5000
        assertEquals(listOf("item5", "pay10"), r.appliedOffers.map { it.offerId }.sorted())
        val aOffers = r.lines.first { it.itemNumber == "A" }.offers.map { it.offerId to it.discountFils }.sortedBy { it.first }
        assertEquals(listOf("item5" to 500L, "pay10" to 1000L), aOffers)
        val bOffers = r.lines.first { it.itemNumber == "B" }.offers
        assertEquals(1, bOffers.size)
        assertEquals("pay10", bOffers[0].offerId)
    }
}
