package com.jehadalomour.flowvan.core.domain.offers

import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.model.OfferEligibilityRule
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferTrigger
import com.jehadalomour.flowvan.core.model.OfferType
import com.jehadalomour.flowvan.core.network.dto.AppliedOfferDto
import com.jehadalomour.flowvan.core.network.dto.EvalLineDto
import com.jehadalomour.flowvan.core.network.dto.EvaluationResultDto
import com.jehadalomour.flowvan.core.network.dto.FreeItemChoiceDto
import com.jehadalomour.flowvan.core.network.dto.FreeItemDto
import com.jehadalomour.flowvan.core.network.dto.FreeLineDto
import com.jehadalomour.flowvan.core.network.dto.LineOfferDto
import com.jehadalomour.flowvan.core.network.dto.TotalsDto
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * On-device port of the backend `OffersEngineService` (cash-van `offers-engine.service.ts`,
 * branch deploy/render). Pure and non-suspend — all inputs are preloaded — so it computes the
 * exact same [EvaluationResultDto] (integer fils) the server returns, letting the app show
 * correct offer prices offline. The server re-applies authoritatively on sync.
 *
 * Money is integer fils. `roundFils` = [Double.roundToLong] (ties toward +∞ = JS `Math.round`
 * for the non-negative values here).
 */
object LocalOfferEvaluator {

    data class ItemInfo(val priceFils: Long, val taxPct: Double)

    data class Context(
        val customerNumber: String?,
        val customerCategory: String?,
        val customerRegionId: String?,
        val customerRepId: String?,
        val repId: String?,
        val storeNumber: String?,
        /** CASH|CHEQUE|TRANSFER|CARD|CREDIT — only CREDIT is treated as credit. */
        val paymentMethod: String?,
        val chosenFreeItems: List<String>,
        /** Local approximation of "no prior SALE" (only consulted for NEW_ONLY offers). */
        val isNewCustomer: Boolean,
        /** Wall-clock for daysOfWeek / timeFrom / timeTo gating. */
        val at: LocalDateTime,
        /** Same instant as [at], epoch-ms, for validFrom / validTo gating. */
        val nowMs: Long,
    )

    private class WorkLine(
        val qty: Double,
        val unitPriceFils: Long,
        val taxPct: Double,
        var discountFils: Long,
        val offers: MutableList<LineOfferDto>,
    )

    private class Disc(val offer: OfferDefinition, val pct: Double, val items: Set<String>?)
    private class GiftEntry(val offer: OfferDefinition, val reward: OfferReward.Gift, val freeQty: Int)

    /**
     * @param lines itemNumber → qty (aggregated like the server's toCartMap).
     * @param offers active offers in ranking order (priority DESC, createdAt ASC).
     * @param items unit price + tax% for every cart SKU AND every gift-pool SKU.
     * @param taxInclusive company tax mode: true → unit prices already include tax
     *   (extract it, don't add on top), matching the backend offers engine + voucher calc.
     */
    fun evaluate(
        lines: List<Pair<String, Double>>,
        offers: List<OfferDefinition>,
        items: Map<String, ItemInfo>,
        ctx: Context,
        taxInclusive: Boolean = false,
    ): EvaluationResultDto {
        val cart = LinkedHashMap<String, Double>()
        for ((itemNumber, qty) in lines) cart[itemNumber] = (cart[itemNumber] ?: 0.0) + qty
        val itemCount = cart.values.sum()

        val work = LinkedHashMap<String, WorkLine>()
        for ((itemNumber, qty) in cart) {
            val info = items[itemNumber]
            work[itemNumber] = WorkLine(
                qty = qty,
                unitPriceFils = info?.priceFils ?: 0L,
                taxPct = info?.taxPct ?: 0.0,
                discountFils = 0L,
                offers = mutableListOf(),
            )
        }
        val subtotalFils = work.values.sumOf { roundFils(it.qty * it.unitPriceFils) }

        val isCredit = ctx.paymentMethod == "CREDIT"
        val payOffers = mutableListOf<Disc>()
        val itemOffers = mutableListOf<Disc>()
        val gifts = mutableListOf<GiftEntry>()

        for (offer in offers) {
            if (!isWithinSchedule(offer, ctx)) continue
            if (!isEligible(offer.eligibility, ctx)) continue
            val redemptionLimit = offer.totalRedemptionLimit
            if (redemptionLimit != null && offer.redemptionCount >= redemptionLimit) continue
            // perCustomerLimit is intentionally NOT checked offline (no local redemption ledger).

            when (offer.type) {
                OfferType.PAYMENT_METHOD_DISCOUNT -> {
                    val t = offer.trigger as? OfferTrigger.PaymentMethod ?: continue
                    val reward = offer.reward as? OfferReward.LinePercent ?: continue
                    val minTotal = t.minOrderTotalFils
                    val minCount = t.minItemCount
                    val payOk = if (t.paymentCondition == "CREDIT") isCredit else !isCredit
                    val totalOk = minTotal == null || subtotalFils >= minTotal
                    val countOk = minCount == null || itemCount >= minCount
                    if (payOk && totalOk && countOk) {
                        val pct = effectivePercent(reward, itemCount, (minCount ?: 0).toDouble())
                        if (pct > 0) payOffers.add(Disc(offer, pct, null))
                    }
                }
                OfferType.ITEM_QTY_REWARD -> {
                    val t = offer.trigger as? OfferTrigger.ItemSet ?: continue
                    val qty = t.itemNumbers.sumOf { cart[it] ?: 0.0 }
                    when (val reward = offer.reward) {
                        is OfferReward.Gift -> {
                            val freeQty = giftFreeQty(reward, qty)
                            if (freeQty > 0) gifts.add(GiftEntry(offer, reward, freeQty))
                        }
                        is OfferReward.ItemPercent -> {
                            if (qty >= reward.minQty) {
                                val pct = effectivePercent(reward, qty, reward.minQty.toDouble())
                                if (pct > 0) itemOffers.add(Disc(offer, pct, t.itemNumbers.toSet()))
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // Highest payment-method % is the same on every line → a single global winner.
        val bestPay = payOffers.reduceOrNull { b, c -> if (c.pct > b.pct) c else b }

        val contrib = LinkedHashMap<String, Long>()
        for ((itemNumber, l) in work) {
            val gross = roundFils(l.qty * l.unitPriceFils)
            if (gross <= 0) continue
            var bestItem: Disc? = null
            for (c in itemOffers) {
                if (c.items!!.contains(itemNumber) && (bestItem == null || c.pct > bestItem.pct)) {
                    bestItem = c
                }
            }
            var payFils = if (bestPay != null) roundFils(gross * bestPay.pct / 100.0) else 0L
            var itemFils = if (bestItem != null) roundFils(gross * bestItem.pct / 100.0) else 0L
            if (payFils > gross) payFils = gross
            if (payFils + itemFils > gross) itemFils = gross - payFils // never below 0
            l.discountFils = payFils + itemFils
            if (bestPay != null && payFils > 0) {
                contrib[bestPay.offer.id] = (contrib[bestPay.offer.id] ?: 0L) + payFils
                l.offers.add(LineOfferDto(bestPay.offer.id, bestPay.offer.name, bestPay.pct, payFils))
            }
            if (bestItem != null && itemFils > 0) {
                contrib[bestItem.offer.id] = (contrib[bestItem.offer.id] ?: 0L) + itemFils
                l.offers.add(LineOfferDto(bestItem.offer.id, bestItem.offer.name, bestItem.pct, itemFils))
            }
        }

        // Gifts → free lines (rep picks resolved from chosenFreeItems).
        val freeLines = mutableListOf<FreeLineDto>()
        val giftValue = LinkedHashMap<String, Long>()
        for (g in gifts) {
            val picks = ctx.chosenFreeItems.filter { it in g.reward.giftItems }.take(g.freeQty)
            for (itemNumber in picks) {
                val priceFils = items[itemNumber]?.priceFils ?: 0L
                freeLines.add(FreeLineDto(itemNumber, 1.0, priceFils, g.offer.id))
                giftValue[g.offer.id] = (giftValue[g.offer.id] ?: 0L) + priceFils
            }
        }

        // Applied-offers summary (discount contributors + gift offers), in offer order.
        val appliedIds = (contrib.keys + gifts.map { it.offer.id }).toSet()
        val appliedOffers = mutableListOf<AppliedOfferDto>()
        for (offer in offers) {
            if (offer.id !in appliedIds) continue
            val gift = gifts.find { it.offer.id == offer.id }
            val picked = gift?.let { g ->
                ctx.chosenFreeItems.filter { it in g.reward.giftItems }.take(g.freeQty)
                    .map { FreeItemDto(it, 1) }
            } ?: emptyList()
            appliedOffers.add(
                AppliedOfferDto(
                    offerId = offer.id,
                    name = offer.name,
                    type = offer.type.name,
                    summary = summarize(offer),
                    discountFils = (contrib[offer.id] ?: 0L) + (giftValue[offer.id] ?: 0L),
                    freeItems = picked,
                    freeItemChoice = gift?.let { FreeItemChoiceDto(it.reward.giftItems, it.freeQty) },
                )
            )
        }

        // Clamp per line, then build the response.
        val resultLines = mutableListOf<EvalLineDto>()
        var lineDiscountTotal = 0L
        var taxFils = 0L
        for ((itemNumber, l) in work) {
            val gross = roundFils(l.qty * l.unitPriceFils)
            val discount = min(l.discountFils, gross)
            val net = gross - discount
            lineDiscountTotal += discount
            taxFils += if (taxInclusive) {
                net - (if (l.taxPct > 0.0) roundFils(net * 100.0 / (100.0 + l.taxPct)) else net)
            } else {
                roundFils(net * (l.taxPct / 100.0))
            }
            resultLines.add(
                EvalLineDto(
                    itemNumber = itemNumber,
                    qty = l.qty,
                    unitPriceFils = l.unitPriceFils,
                    lineDiscountFils = discount,
                    lineNetFils = net,
                    offers = l.offers.toList(),
                )
            )
        }
        val invoiceDiscountFils = 0L // no invoice-level offers in the current model
        // INCLUSIVE: tax is already inside the net → don't add it on top.
        val grandTotalFils =
            (subtotalFils - lineDiscountTotal) + (if (taxInclusive) 0L else taxFils) - invoiceDiscountFils

        return EvaluationResultDto(
            lines = resultLines,
            freeLines = freeLines,
            invoiceDiscountFils = invoiceDiscountFils,
            appliedOffers = appliedOffers,
            totals = TotalsDto(
                subtotalFils = subtotalFils,
                lineDiscountFils = lineDiscountTotal,
                invoiceDiscountFils = invoiceDiscountFils,
                totalDiscountFils = lineDiscountTotal + invoiceDiscountFils,
                taxFils = taxFils,
                grandTotalFils = grandTotalFils,
            ),
        )
    }

    private fun roundFils(n: Double): Long = n.roundToLong()

    private fun giftFreeQty(reward: OfferReward.Gift, qty: Double): Int {
        if (reward.itemsPerGift < 1) return 0
        val perStep = if (reward.giftsPerStep > 0) reward.giftsPerStep else 1
        val n = floor(qty / reward.itemsPerGift).toInt() * perStep
        return reward.maxFreeQty?.let { min(n, it) } ?: n
    }

    /**
     * Base percent applies at the offer's threshold ([anchor]); each full itemsPerStep ABOVE it
     * adds one multiplier step. STATIC ignores the multiplier. Capped at min(maxPercent, 100).
     */
    private fun effectivePercent(reward: OfferReward.LinePercent, count: Double, anchor: Double): Double =
        effectivePercent(reward.basePercent, reward.dynamic, reward.multiplier, reward.itemsPerStep, reward.maxPercent, count, anchor)

    private fun effectivePercent(reward: OfferReward.ItemPercent, count: Double, anchor: Double): Double =
        effectivePercent(reward.basePercent, reward.dynamic, reward.multiplier, reward.itemsPerStep, reward.maxPercent, count, anchor)

    private fun effectivePercent(
        basePercent: Double,
        dynamic: Boolean,
        multiplier: Double?,
        itemsPerStep: Int?,
        maxPercent: Double?,
        count: Double,
        anchor: Double,
    ): Double {
        val steps = if (dynamic && itemsPerStep != null && itemsPerStep > 0)
            floor(max(0.0, count - anchor) / itemsPerStep) else 0.0
        val pct = basePercent * (1 + (multiplier ?: 0.0) * steps)
        val cap = min(maxPercent ?: 100.0, 100.0)
        return max(0.0, min(pct, cap))
    }

    private fun isWithinSchedule(offer: OfferDefinition, ctx: Context): Boolean {
        val from = offer.validFromMs
        if (from != null && ctx.nowMs < from) return false
        val to = offer.validToMs
        if (to != null && ctx.nowMs > to) return false
        val days = offer.daysOfWeek
        if (days != null && days.isNotEmpty()) {
            // isoDayNumber: Mon=1..Sun=7 → % 7 → Sun=0..Sat=6 (JS getDay()).
            if (ctx.at.dayOfWeek.isoDayNumber % 7 !in days) return false
        }
        val timeFrom = offer.timeFrom
        val timeTo = offer.timeTo
        if (timeFrom != null || timeTo != null) {
            val hhmm = pad2(ctx.at.hour) + ":" + pad2(ctx.at.minute)
            if (timeFrom != null && hhmm < timeFrom) return false
            if (timeTo != null && hhmm > timeTo) return false
        }
        return true
    }

    private fun isEligible(e: OfferEligibilityRule, ctx: Context): Boolean {
        when (e.customerScope) {
            "ALL" -> Unit
            "SEGMENT" ->
                if (ctx.customerCategory == null || e.segments?.contains(ctx.customerCategory) != true) return false
            "SPECIFIC" ->
                if (ctx.customerNumber == null || e.customerNumbers?.contains(ctx.customerNumber) != true) return false
            "NEW_ONLY" ->
                if (ctx.customerNumber == null || !ctx.isNewCustomer) return false
            else -> Unit
        }
        val regionIds = e.regionIds
        if (!regionIds.isNullOrEmpty()) {
            if (ctx.customerRegionId == null || ctx.customerRegionId !in regionIds) return false
        }
        val repIds = e.repIds
        if (!repIds.isNullOrEmpty()) {
            val rep = ctx.repId ?: ctx.customerRepId
            if (rep == null || rep !in repIds) return false
        }
        val storeNumbers = e.storeNumbers
        if (!storeNumbers.isNullOrEmpty()) {
            if (ctx.storeNumber == null || ctx.storeNumber !in storeNumbers) return false
        }
        return true
    }

    /** Human-readable one-liner for the applied-offers banner — ports the server's summarize(). */
    private fun summarize(offer: OfferDefinition): String {
        if (offer.type == OfferType.ITEM_QTY_REWARD) {
            val on = (offer.trigger as? OfferTrigger.ItemSet)?.itemNumbers?.joinToString("/") ?: ""
            return when (val r = offer.reward) {
                is OfferReward.Gift -> {
                    val cap = r.maxFreeQty?.let { " (max $it)" } ?: ""
                    val per = if (r.giftsPerStep > 1) "${r.giftsPerStep} gifts" else "1 gift"
                    "Buy $on: $per / ${r.itemsPerGift} bought$cap, from ${r.giftItems.size} items"
                }
                is OfferReward.ItemPercent -> {
                    val base = if (r.dynamic)
                        "${numStr(r.basePercent)}%→${numStr(r.maxPercent ?: r.basePercent)}% dynamic"
                    else "${numStr(r.basePercent)}%"
                    "Buy ${r.minQty}× $on → $base off those items"
                }
                else -> offer.name
            }
        }
        val t = offer.trigger as? OfferTrigger.PaymentMethod
        val r = offer.reward as? OfferReward.LinePercent
        val cond = if (t?.paymentCondition == "CREDIT") "Credit" else "Cash"
        val mins = mutableListOf<String>()
        t?.minOrderTotalFils?.takeIf { it != 0L }?.let { mins.add("≥ ${jodString(it)} JOD") }
        t?.minItemCount?.takeIf { it != 0 }?.let { mins.add("≥ $it items") }
        val suffix = if (mins.isNotEmpty()) " (${mins.joinToString(", ")})" else ""
        if (r?.dynamic == true) {
            val cap = r.maxPercent?.let { " up to ${numStr(it)}%" } ?: ""
            val step = r.itemsPerStep?.toString() ?: "?"
            return "$cond · ${numStr(r.basePercent)}% per line, ×${numStr(r.multiplier ?: 0.0)} per $step items$cap$suffix"
        }
        return "$cond · ${numStr(r?.basePercent ?: 0.0)}% off each line$suffix"
    }

    private fun pad2(n: Int): String = if (n < 10) "0$n" else n.toString()

    /** fils → "6.000" JOD string (matches JS `(fils/1000).toFixed(3)` for non-negative fils). */
    private fun jodString(fils: Long): String = "${fils / 1000}.${(fils % 1000).toString().padStart(3, '0')}"

    /** Prints a whole Double without a trailing ".0" (matches JS number→string). */
    private fun numStr(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
}
