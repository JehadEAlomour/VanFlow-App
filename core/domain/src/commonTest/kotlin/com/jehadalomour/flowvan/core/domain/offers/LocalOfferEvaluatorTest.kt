package com.jehadalomour.flowvan.core.domain.offers

import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.model.OfferEligibilityRule
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferTrigger
import com.jehadalomour.flowvan.core.model.OfferType
import com.jehadalomour.flowvan.core.model.TableEntry
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

    private fun ctx(
        payment: String? = null,
        chosen: List<String> = emptyList(),
        customerNumber: String? = null,
    ) =
        LocalOfferEvaluator.Context(
            customerNumber = customerNumber, customerCategory = null, customerRegionId = null,
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
        bundle: Boolean = false,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.ITEM_QTY_REWARD,
        trigger = OfferTrigger.ItemSet(itemNumbers, paymentCondition),
        reward = OfferReward.ItemAmount(minQty, baseAmountFils, dynamic, multiplier, itemsPerStep, maxAmountFils, maxPercentOfPrice, bundle),
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

    /**
     * BUNDLE = lump sum per completed group, NOT a per-unit rate: the discount only moves when a
     * new group of `itemsPerStep` completes, so 2 and 3 tie, then 4 and 5 tie. Mirrors the live
     * "ال دي سلفر" offer (1.05 JOD per 2 bought).
     */
    @Test
    fun itemAmountBundlePaysLumpSumPerCompletedGroup() {
        val offer = itemAmtOffer(
            itemNumbers = listOf("A"), minQty = 2, baseAmountFils = 1050.0,
            bundle = true, itemsPerStep = 2,
        )
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).disc("A")
        assertEquals(1050, d(2.0))
        assertEquals(1050, d(3.0)) // partial pair earns nothing extra
        assertEquals(2100, d(4.0))
        assertEquals(2100, d(5.0))
        assertEquals(3150, d(6.0))
        // Below minQty the offer does not apply at all.
        assertTrue(LocalOfferEvaluator.evaluate(listOf("A" to 1.0), listOf(offer), items, ctx()).appliedOffers.isEmpty())
    }

    /**
     * Reported from the field: an offer set to start at 1 paid nothing until the 2nd
     * unit, because groups were counted as floor(qty / itemsPerStep) and so ignored
     * minQty. The first group must land ON minQty.
     */
    @Test
    fun itemAmountBundleWithMinQtyOnePaysFromTheFirstUnit() {
        val offer = itemAmtOffer(
            // 400, not the live 1050: at qty 1 the line gross is 1000 fils and the
            // per-line clamp would cap the discount and hide the bug.
            itemNumbers = listOf("A"), minQty = 1, baseAmountFils = 400.0,
            bundle = true, itemsPerStep = 2,
        )
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).disc("A")
        assertEquals(400, d(1.0)) // was 0
        assertEquals(400, d(2.0))
        assertEquals(800, d(3.0)) // one full step past the anchor
        assertEquals(1200, d(5.0))
    }

    /** minQty 1 with a step of 1 pays on every single unit. */
    @Test
    fun itemAmountBundleWithStepOnePaysEveryUnit() {
        val offer = itemAmtOffer(
            itemNumbers = listOf("A"), minQty = 1, baseAmountFils = 500.0,
            bundle = true, itemsPerStep = 1,
        )
        fun d(q: Double) = LocalOfferEvaluator.evaluate(listOf("A" to q), listOf(offer), items, ctx()).disc("A")
        assertEquals(500, d(1.0))
        assertEquals(1500, d(3.0))
    }

    /** The lump sum is shared across the trigger's items, not paid once per line. */
    @Test
    fun itemAmountBundleSpreadsOneLumpSumAcrossTheItemSet() {
        val offer = itemAmtOffer(
            itemNumbers = listOf("A", "B"), minQty = 2, baseAmountFils = 400.0,
            bundle = true, itemsPerStep = 2,
        )
        // Combined qty 4 → 2 groups → 800 total, split proportionally (A×2, B×2) — not 800 each.
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 2.0, "B" to 2.0), listOf(offer), items, ctx())
        assertEquals(400, r.disc("A"))
        assertEquals(400, r.disc("B"))
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

    // ── quantity bands (min..max) + per-item "dynamic" tables ────────────────

    private fun bandedAmtOffer(
        id: String, condition: String = "CASH", baseAmountFils: Double,
        minItemCount: Int? = null, maxItemCount: Int? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.PAYMENT_METHOD_DISCOUNT,
        trigger = OfferTrigger.PaymentMethod(condition, null, minItemCount, maxItemCount),
        reward = OfferReward.LineAmount(baseAmountFils, false, null, null, null, null),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    private fun tableAmtOffer(
        id: String, condition: String = "CASH", entries: List<TableEntry>,
        minItemCount: Int? = null, maxItemCount: Int? = null,
        eligibility: OfferEligibilityRule = allEligibility,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.PAYMENT_METHOD_DISCOUNT,
        trigger = OfferTrigger.PaymentMethod(condition, null, minItemCount, maxItemCount),
        reward = OfferReward.TableAmount(entries),
        eligibility = eligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    /**
     * A customer-scoped offer must beat a general one for its customer even when it discounts
     * LESS — otherwise scoping an offer to a customer has no effect. Mirrors the live setup
     * where the same item was covered by both a general and a customer-specific table offer.
     */
    @Test
    fun customerSpecificOfferBeatsGeneralOfferEvenWhenLower() {
        val general = tableAmtOffer(
            id = "general", entries = listOf(TableEntry("A", amountFils = 300.0)),
        )
        val specific = tableAmtOffer(
            id = "specific", entries = listOf(TableEntry("A", amountFils = 200.0)),
            eligibility = OfferEligibilityRule("SPECIFIC", null, listOf("1370"), null, null, null),
        )
        val r = LocalOfferEvaluator.evaluate(
            listOf("A" to 2.0), listOf(general, specific), items, ctx("CASH", customerNumber = "1370"),
        )
        assertEquals(400, r.disc("A")) // 200/unit × 2, NOT 600
        assertEquals(listOf("specific"), r.appliedOffers.map { it.offerId })
    }

    /** A customer the specific offer does not target still gets the general offer. */
    @Test
    fun generalOfferStillAppliesToUntargetedCustomer() {
        val general = tableAmtOffer(
            id = "general", entries = listOf(TableEntry("A", amountFils = 300.0)),
        )
        val specific = tableAmtOffer(
            id = "specific", entries = listOf(TableEntry("A", amountFils = 200.0)),
            eligibility = OfferEligibilityRule("SPECIFIC", null, listOf("1370"), null, null, null),
        )
        val r = LocalOfferEvaluator.evaluate(
            listOf("A" to 2.0), listOf(general, specific), items, ctx("CASH", customerNumber = "9999"),
        )
        assertEquals(600, r.disc("A")) // 300/unit × 2
        assertEquals(listOf("general"), r.appliedOffers.map { it.offerId })
    }

    private fun tablePctOffer(
        id: String, condition: String = "CASH", entries: List<TableEntry>,
        minItemCount: Int? = null, maxItemCount: Int? = null,
    ) = OfferDefinition(
        id = id, name = id, type = OfferType.PAYMENT_METHOD_DISCOUNT,
        trigger = OfferTrigger.PaymentMethod(condition, null, minItemCount, maxItemCount),
        reward = OfferReward.TablePercent(entries),
        eligibility = allEligibility,
        validFromMs = null, validToMs = null, daysOfWeek = null, timeFrom = null, timeTo = null,
        totalRedemptionLimit = null, perCustomerLimit = null, priority = 0, redemptionCount = 0, createdAtMs = 0,
    )

    @Test
    fun quantityBandAppliesOnlyInsideInterval() {
        val offers = listOf(
            bandedAmtOffer("band1", baseAmountFils = 100.0, minItemCount = 1, maxItemCount = 49),
            bandedAmtOffer("band2", baseAmountFils = 200.0, minItemCount = 50, maxItemCount = 99),
        )
        val at49 = LocalOfferEvaluator.evaluate(listOf("A" to 49.0), offers, items, ctx("CASH"))
        assertEquals(listOf("band1"), at49.appliedOffers.map { it.offerId })
        assertEquals(4900, at49.disc("A")) // 100 × 49
        val at50 = LocalOfferEvaluator.evaluate(listOf("A" to 50.0), offers, items, ctx("CASH"))
        assertEquals(listOf("band2"), at50.appliedOffers.map { it.offerId })
        assertEquals(10000, at50.disc("A")) // 200 × 50
        val at100 = LocalOfferEvaluator.evaluate(listOf("A" to 100.0), offers, items, ctx("CASH"))
        assertEquals(0, at100.appliedOffers.size)
    }

    @Test
    fun tableAmountPerItemUnlistedGetsNothing() {
        val offers = listOf(
            tableAmtOffer("tbl", entries = listOf(TableEntry("A", amountFils = 110.0)), minItemCount = 50, maxItemCount = 99),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 50.0, "B" to 5.0), offers, items, ctx("CASH"))
        assertEquals(5500, r.disc("A")) // 110 × 50
        assertEquals(0, r.disc("B"))    // B unlisted → nothing
        val below = LocalOfferEvaluator.evaluate(listOf("A" to 40.0, "B" to 5.0), offers, items, ctx("CASH"))
        assertEquals(0, below.appliedOffers.size) // 45 < band floor
    }

    @Test
    fun tablePercentPerItem() {
        val offers = listOf(
            tablePctOffer("tblp", entries = listOf(TableEntry("A", percent = 10.0), TableEntry("B", percent = 20.0)), minItemCount = 1, maxItemCount = 99),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 50.0, "B" to 10.0), offers, items, ctx("CASH"))
        assertEquals(5000, r.disc("A")) // 50000 × 10%
        assertEquals(1000, r.disc("B")) // 5000 × 20%
    }

    @Test
    fun tableAmountCapsAtMaxPercentOfPrice() {
        val offers = listOf(
            tableAmtOffer("tblcap", entries = listOf(TableEntry("A", amountFils = 500.0, maxPercentOfPrice = 20.0)), minItemCount = 1, maxItemCount = 99),
        )
        val r = LocalOfferEvaluator.evaluate(listOf("A" to 50.0), offers, items, ctx("CASH"))
        assertEquals(10000, r.disc("A")) // 200 (20% of 1000) × 50
    }
}
