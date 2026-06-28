package com.jehadalomour.flowvan.core.model

import kotlin.math.roundToLong

/**
 * Canonical voucher money engine — the Kotlin twin of the backend's
 * `voucher-calc.ts`. Produces byte-for-fil identical results so the app's offline
 * totals match what the dashboard (authority) and the ERP compute. See
 * cash-van-dashboard/docs/VOUCHER-CALC-SPEC.md.
 *
 * All amounts are integer **fils** (JOD × 1000). Quantities may be fractional.
 * Rounding: `roundToLong()` (ties → +infinity, same as JS `Math.round` for the
 * non-negative money values used here).
 */
enum class TaxMode { INCLUSIVE, EXCLUSIVE }

data class CalcLineInput(
    val unitPriceFils: Long,
    val qty: Double,
    val lineDiscountPct: Double = 0.0, // 0..100
    val lineDiscountFils: Long = 0L,
    val taxRatePct: Double = 0.0,      // e.g. 16
)

data class CalcInput(
    val lines: List<CalcLineInput>,
    val taxMode: TaxMode,
    val headerDiscountPct: Double = 0.0,
    val headerDiscountFils: Long = 0L,
)

data class CalcLineResult(
    val grossFils: Long,
    val lineDiscountFils: Long,
    val netBeforeHeaderFils: Long,
    val headerShareFils: Long,
    val netFils: Long,
    val taxableFils: Long,
    val taxFils: Long,
    val totalFils: Long,
)

data class CalcResult(
    val lines: List<CalcLineResult>,
    val totalNetFils: Long,
    val totalTaxFils: Long,
    val grandTotalFils: Long,
    val totalLineDiscountFils: Long,
    val headerDiscountFils: Long,
    val totalDiscountFils: Long,
)

object VoucherCalc {

    fun calc(input: CalcInput): CalcResult {
        // 1. per-line gross + line discount (% then fixed, stacked, clamped to gross)
        val gross = LongArray(input.lines.size)
        val lineDisc = LongArray(input.lines.size)
        val netBefore = LongArray(input.lines.size)
        input.lines.forEachIndexed { i, l ->
            val g = (l.unitPriceFils.toDouble() * l.qty).roundToLong()
            val fromPct = (g.toDouble() * (l.lineDiscountPct / 100.0)).roundToLong()
            val fixed = if (l.lineDiscountFils > 0L) l.lineDiscountFils else 0L
            val d = (fromPct + fixed).coerceIn(0L, g)
            gross[i] = g
            lineDisc[i] = d
            netBefore[i] = g - d
        }

        // 2. header discount (pct of net, or fixed) distributed by net share
        val sumNet = netBefore.sum()
        val headerRaw = if (input.headerDiscountPct > 0.0) {
            (sumNet.toDouble() * (input.headerDiscountPct / 100.0)).roundToLong()
        } else {
            if (input.headerDiscountFils > 0L) input.headerDiscountFils else 0L
        }
        val headerDisc = headerRaw.coerceIn(0L, sumNet)
        val shares = LongArray(input.lines.size) { i ->
            if (sumNet > 0L) {
                (headerDisc.toDouble() * (netBefore[i].toDouble() / sumNet.toDouble())).roundToLong()
            } else 0L
        }
        val diff = headerDisc - shares.sum()
        if (diff != 0L) {
            for (i in input.lines.indices.reversed()) {
                if (netBefore[i] > 0L) { shares[i] += diff; break }
            }
        }

        // 3. tax per line (after discounts), inclusive or exclusive
        val lines = input.lines.mapIndexed { i, l ->
            val headerShare = shares[i]
            val net = netBefore[i] - headerShare
            val rate = l.taxRatePct
            val taxable: Long
            val tax: Long
            val total: Long
            if (input.taxMode == TaxMode.INCLUSIVE) {
                taxable = if (rate > 0.0) (net.toDouble() * 100.0 / (100.0 + rate)).roundToLong() else net
                tax = net - taxable
                total = net
            } else {
                taxable = net
                tax = if (rate > 0.0) (net.toDouble() * rate / 100.0).roundToLong() else 0L
                total = net + tax
            }
            CalcLineResult(
                grossFils = gross[i],
                lineDiscountFils = lineDisc[i],
                netBeforeHeaderFils = netBefore[i],
                headerShareFils = headerShare,
                netFils = net,
                taxableFils = taxable,
                taxFils = tax,
                totalFils = total,
            )
        }

        val totalLineDiscount = lines.sumOf { it.lineDiscountFils }
        return CalcResult(
            lines = lines,
            totalNetFils = lines.sumOf { it.taxableFils },
            totalTaxFils = lines.sumOf { it.taxFils },
            grandTotalFils = lines.sumOf { it.totalFils },
            totalLineDiscountFils = totalLineDiscount,
            headerDiscountFils = headerDisc,
            totalDiscountFils = totalLineDiscount + headerDisc,
        )
    }
}

/** JOD major → integer fils. */
fun Double.toFils(): Long = (this * 1000.0).roundToLong()

/** Integer fils → JOD major. */
fun Long.filsToJod(): Double = this / 1000.0
