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

    /**
     * [amountPerUnitFils] set → an amount reward (fils off per unit, × line qty) —
     * LINE_AMOUNT_DISCOUNT (all lines) or ITEM_AMOUNT_DISCOUNT (selected lines);
     * [maxPercentOfPrice] optionally caps the per-unit amount to a % of the line's unit
     * price. null → percent via [pct].
     */
    private class Disc(
        val offer: OfferDefinition,
        val pct: Double,
        val items: Set<String>?,
        val amountPerUnitFils: Double? = null,
        val maxPercentOfPrice: Double? = null,
        /**
         * Per-item discount table (TABLE_*_DISCOUNT, the "dynamic" payment-method offer).
         * When set, the discount is looked up PER LINE from the item's cell; a line whose
         * itemNumber is absent contributes nothing. Overrides [pct]/[amountPerUnitFils].
         */
        val table: Map<String, TableCell>? = null,
    )
    private class TableCell(
        val pct: Double = 0.0,
        val amountPerUnitFils: Double? = null,
        val maxPercentOfPrice: Double? = null,
    )
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
                    val minTotal = t.minOrderTotalFils
                    val minCount = t.minItemCount
                    val maxCount = t.maxItemCount
                    val payOk = if (t.paymentCondition == "CREDIT") isCredit else !isCredit
                    val totalOk = minTotal == null || subtotalFils >= minTotal
                    val countOk = minCount == null || itemCount >= minCount
                    // Quantity band ceiling: the order's total unit count must be within
                    // [minItemCount, maxItemCount]. Null max = open-ended (legacy).
                    val maxOk = maxCount == null || itemCount <= maxCount
                    if (payOk && totalOk && countOk && maxOk) {
                        val anchor = (minCount ?: 0).toDouble()
                        when (val reward = offer.reward) {
                            is OfferReward.LinePercent -> {
                                val pct = effectivePercent(reward, itemCount, anchor)
                                if (pct > 0) payOffers.add(Disc(offer, pct, null))
                            }
                            is OfferReward.LineAmount -> {
                                // Per-unit amount on every line (× line qty), like the server.
                                val amt = effectiveAmount(reward, itemCount, anchor)
                                if (amt > 0) payOffers.add(
                                    Disc(offer, 0.0, null, amountPerUnitFils = amt, maxPercentOfPrice = reward.maxPercentOfPrice),
                                )
                            }
                            is OfferReward.TableAmount -> {
                                // Per-item table ("dynamic"): each listed item its own per-unit
                                // amount off; unlisted items absent from the map → no discount.
                                val table = LinkedHashMap<String, TableCell>()
                                for (e in reward.entries) {
                                    val amt = e.amountFils
                                    if (amt != null && amt > 0) {
                                        table[e.itemNumber] = TableCell(amountPerUnitFils = amt, maxPercentOfPrice = e.maxPercentOfPrice)
                                    }
                                }
                                if (table.isNotEmpty()) payOffers.add(Disc(offer, 0.0, null, table = table))
                            }
                            is OfferReward.TablePercent -> {
                                val table = LinkedHashMap<String, TableCell>()
                                for (e in reward.entries) {
                                    val pct = e.percent
                                    if (pct != null && pct > 0) {
                                        table[e.itemNumber] = TableCell(pct = min(pct, 100.0))
                                    }
                                }
                                if (table.isNotEmpty()) payOffers.add(Disc(offer, 0.0, null, table = table))
                            }
                            else -> {}
                        }
                    }
                }
                OfferType.ITEM_QTY_REWARD -> {
                    val t = offer.trigger as? OfferTrigger.ItemSet ?: continue
                    // Optional payment gate (CASH = any non-CREDIT). Null = any payment.
                    val cond = t.paymentCondition
                    if (cond != null && (if (cond == "CREDIT") !isCredit else isCredit)) continue
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
                        is OfferReward.ItemAmount -> {
                            if (qty >= reward.minQty) {
                                val amt = effectiveAmount(reward, qty, reward.minQty.toDouble())
                                if (amt > 0) itemOffers.add(
                                    Disc(offer, 0.0, t.itemNumbers.toSet(), amt, reward.maxPercentOfPrice),
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        val contrib = LinkedHashMap<String, Long>()
        for ((itemNumber, l) in work) {
            val gross = roundFils(l.qty * l.unitPriceFils)
            if (gross <= 0) continue
            // Candidate's discount for this line: per-unit amount → fils × line qty (the
            // per-unit amount first clamped to maxPercentOfPrice of THIS line's unit price,
            // when set); percent → % of gross. Highest fils wins per line, so a percent and an
            // amount offer compare correctly.
            fun discFor(c: Disc): Long {
                // Per-item table candidates resolve their value from THIS line's cell; a line
                // not listed in the table contributes nothing from this offer.
                var pct = c.pct
                var perUnitAmt = c.amountPerUnitFils
                var capPct = c.maxPercentOfPrice
                val table = c.table
                if (table != null) {
                    val cell = table[itemNumber] ?: return 0L
                    pct = cell.pct
                    perUnitAmt = cell.amountPerUnitFils
                    capPct = cell.maxPercentOfPrice
                }
                if (perUnitAmt != null) {
                    var perUnit = perUnitAmt
                    if (capPct != null) {
                        val cap = roundFils(l.unitPriceFils * capPct / 100.0).toDouble()
                        if (cap < perUnit) perUnit = cap
                    }
                    return roundFils(l.qty * perUnit)
                }
                return roundFils(gross * pct / 100.0)
            }
            var bestPay: Disc? = null
            var bestPayFils = 0L
            var bestPayRank = -1
            for (c in payOffers) {
                val d = discFor(c)
                val rank = scopeRank(c.offer)
                if (bestPay == null || rank > bestPayRank || (rank == bestPayRank && d > bestPayFils)) {
                    bestPay = c
                    bestPayFils = d
                    bestPayRank = rank
                }
            }
            var bestItem: Disc? = null
            var bestItemFils = 0L
            var bestItemRank = -1
            for (c in itemOffers) {
                if (!c.items!!.contains(itemNumber)) continue
                val d = discFor(c)
                val rank = scopeRank(c.offer)
                if (bestItem == null || rank > bestItemRank || (rank == bestItemRank && d > bestItemFils)) {
                    bestItem = c
                    bestItemFils = d
                    bestItemRank = rank
                }
            }
            var payFils = if (bestPay != null) bestPayFils else 0L
            var itemFils = if (bestItem != null) bestItemFils else 0L
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

    /**
     * Effective amount (fils) for an amount-off reward — the fils twin of [effectivePercent],
     * shared by LINE_AMOUNT_DISCOUNT (per line) and ITEM_AMOUNT_DISCOUNT (per unit). Base applies
     * at [anchor] (minItemCount or minQty); each full itemsPerStep above it adds one step. STATIC
     * ignores the multiplier. Capped at maxAmountFils (no natural bound otherwise); the per-line
     * clamp to the line gross still prevents a negative net.
     */
    private fun effectiveAmount(reward: OfferReward.ItemAmount, count: Double, anchor: Double): Double =
        effectiveAmount(reward.baseAmountFils, reward.dynamic, reward.multiplier, reward.itemsPerStep, reward.maxAmountFils, count, anchor, reward.bundle)

    private fun effectiveAmount(reward: OfferReward.LineAmount, count: Double, anchor: Double): Double =
        effectiveAmount(reward.baseAmountFils, reward.dynamic, reward.multiplier, reward.itemsPerStep, reward.maxAmountFils, count, anchor, reward.bundle)

    /**
     * [bundle] pays a LUMP SUM per completed group instead of a per-unit rate: the
     * first group lands at the reward's minQty and each itemsPerStep above it adds
     * another, capped at maxAmountFils as a TOTAL. It is
     * returned divided by [count] so the caller's per-line `perUnit × lineQty` machinery spreads
     * the lump sum across the offer's lines proportionally; the division stays fractional on
     * purpose so rounding happens once, on each line's fils total. Mirrors the server's
     * OffersEngineService.effectiveAmount().
     */
    private fun effectiveAmount(
        baseAmountFils: Double,
        dynamic: Boolean,
        multiplier: Double?,
        itemsPerStep: Int?,
        maxAmountFils: Double?,
        count: Double,
        anchor: Double,
        bundle: Boolean = false,
    ): Double {
        if (bundle) {
            val per = if (itemsPerStep != null && itemsPerStep > 0) itemsPerStep else 1
            // The FIRST group completes at [anchor] (the reward's minQty), and every
            // [per] items after that adds another. Plain floor(count / per) ignored
            // minQty, so an offer set to start at 1 with a step of 2 paid nothing
            // until the 2nd unit. Falling back to [per] when there is no anchor
            // keeps the old floor(count / per) behaviour exactly.
            val firstAt = if (anchor >= 1.0) anchor else per.toDouble()
            val groups = if (count < firstAt) 0.0 else 1.0 + floor((count - firstAt) / per)
            if (groups <= 0.0 || count <= 0.0) return 0.0
            val total = baseAmountFils * groups
            val cap = maxAmountFils ?: Double.POSITIVE_INFINITY
            return max(0.0, min(total, cap)) / count
        }
        val steps = if (dynamic && itemsPerStep != null && itemsPerStep > 0)
            floor(max(0.0, count - anchor) / itemsPerStep) else 0.0
        val amt = baseAmountFils * (1 + (multiplier ?: 0.0) * steps)
        val cap = maxAmountFils ?: Double.POSITIVE_INFINITY
        return max(0.0, min(amt, cap))
    }

    /**
     * Conflict-resolution priority for the per-line "best offer wins" comparison: a targeted
     * offer (SPECIFIC/SEGMENT/NEW_ONLY) always beats a generic ALL offer in the same category
     * (payment-method vs item), regardless of discount value — amount only breaks a tie WITHIN
     * the same rank. Without this a customer-specific offer with the same or lower amount than a
     * general one could never win, defeating the point of scoping an offer to a customer.
     * Mirrors the server's OffersEngineService.scopeRank().
     */
    private fun scopeRank(offer: OfferDefinition): Int =
        if (offer.eligibility.customerScope == "ALL") 0 else 1

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
                is OfferReward.ItemAmount -> if (r.bundle) {
                    "Buy $on → ${jodString(r.baseAmountFils.roundToLong())} JOD off per ${r.itemsPerStep ?: 1} bought"
                } else {
                    val base = if (r.dynamic)
                        "${jodString(r.baseAmountFils.roundToLong())}→${jodString((r.maxAmountFils ?: r.baseAmountFils).roundToLong())} JOD dynamic"
                    else "${jodString(r.baseAmountFils.roundToLong())} JOD"
                    "Buy ${r.minQty}× $on → $base off each unit"
                }
                else -> offer.name
            }
        }
        val t = offer.trigger as? OfferTrigger.PaymentMethod
        val cond = if (t?.paymentCondition == "CREDIT") "Credit" else "Cash"
        val mins = mutableListOf<String>()
        t?.minOrderTotalFils?.takeIf { it != 0L }?.let { mins.add("≥ ${jodString(it)} JOD") }
        // Quantity band: show the interval when a ceiling is set, else the floor only.
        val minC = t?.minItemCount
        val maxC = t?.maxItemCount
        when {
            minC != null && maxC != null -> mins.add("$minC–$maxC items")
            maxC != null -> mins.add("≤ $maxC items")
            minC != null && minC != 0 -> mins.add("≥ $minC items")
        }
        val suffix = if (mins.isNotEmpty()) " (${mins.joinToString(", ")})" else ""
        (offer.reward as? OfferReward.TableAmount)?.let {
            val n = it.entries.size
            return "$cond · per-item amount off ($n item${if (n == 1) "" else "s"})$suffix"
        }
        (offer.reward as? OfferReward.TablePercent)?.let {
            val n = it.entries.size
            return "$cond · per-item % off ($n item${if (n == 1) "" else "s"})$suffix"
        }
        val amount = offer.reward as? OfferReward.LineAmount
        if (amount != null) {
            if (amount.dynamic) {
                val cap = amount.maxAmountFils?.let { " up to ${jodString(it.roundToLong())} JOD" } ?: ""
                val step = amount.itemsPerStep?.toString() ?: "?"
                return "$cond · ${jodString(amount.baseAmountFils.roundToLong())} JOD per unit, ×${numStr(amount.multiplier ?: 0.0)} per $step items$cap$suffix"
            }
            return "$cond · ${jodString(amount.baseAmountFils.roundToLong())} JOD off each unit$suffix"
        }
        val r = offer.reward as? OfferReward.LinePercent
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
