package com.jehadalomour.flowvan.core.model

import kotlin.math.roundToLong

/** Per-line tobacco tax breakdown, in integer fils. */
data class TobaccoTaxResult(
    val salesTaxFils: Long,
    val specialTaxFils: Long,
    val withheldTaxFils: Long,
    val grossTaxFils: Long, // salesTax + specialTax
    val netTaxFils: Long,   // gross - withheld, floored at 0
)

/**
 * Tobacco Tax Engine — Kotlin twin of
 * cash-van-dashboard/src/modules/vouchers/tobacco-tax-calc.ts and the ERP engine.
 * MUST produce the same integer-fils output for the same inputs — do not "improve"
 * the math here alone; change all three engines together (docs/SPEC-tobacco-tax.md).
 *
 * Three components may apply: Sales Tax (on sale or consumer price), Special/excise
 * Tax (fixed per unit, a rate, or fixed+rate), Withheld Tax (prepaid, subtracted).
 * All money is integer fils; quantities may be fractional; round half-up per step
 * (roundToLong ties → +infinity, matching JS Math.round for these non-negative values).
 */
object TobaccoTaxCalc {

    fun calc(
        quantity: Double,
        unitPriceFils: Long, // sale price per base piece (post-discount for SALE_PRICE base)
        consumerPriceFils: Long, // MSRP per base piece
        profile: TobaccoTaxProfile,
    ): TobaccoTaxResult {
        val saleBase = (unitPriceFils.toDouble() * quantity).roundToLong()
        val consumerBase = (consumerPriceFils.toDouble() * quantity).roundToLong()

        // 1. Sales tax
        val salesTaxBase = if (profile.taxBase == TobaccoTaxBase.CONSUMER_PRICE) consumerBase else saleBase
        val salesTax =
            if (profile.salesTaxEnabled && profile.salesTaxRate > 0)
                (salesTaxBase.toDouble() * profile.salesTaxRate / 100.0).roundToLong()
            else 0L

        // 2. Special (excise) tax
        val specialTax = calcSpecial(quantity, saleBase, consumerBase, profile)

        // 3. Withheld tax
        val grossTax = salesTax + specialTax
        val withheldTax = calcWithheld(quantity, saleBase, consumerBase, grossTax, profile)

        val netTax = (grossTax - withheldTax).coerceAtLeast(0L)
        return TobaccoTaxResult(salesTax, specialTax, withheldTax, grossTax, netTax)
    }

    private fun calcSpecial(
        quantity: Double,
        saleBase: Long,
        consumerBase: Long,
        p: TobaccoTaxProfile,
    ): Long {
        if (!p.specialTaxEnabled) return 0L
        return when (p.specialTaxCalculationType) {
            SpecialTaxCalcType.NONE -> 0L
            SpecialTaxCalcType.FIXED_PER_UNIT ->
                ((p.specialTaxFixedAmount ?: 0L).toDouble() * quantity).roundToLong()
            SpecialTaxCalcType.RATE ->
                (specialBase(saleBase, consumerBase, quantity, p) * (p.specialTaxRate ?: 0) / 100.0).roundToLong()
            SpecialTaxCalcType.FIXED_PLUS_RATE ->
                ((p.specialTaxFixedAmount ?: 0L).toDouble() * quantity).roundToLong() +
                    (specialBase(saleBase, consumerBase, quantity, p) * (p.specialTaxRate ?: 0) / 100.0).roundToLong()
        }
    }

    private fun specialBase(saleBase: Long, consumerBase: Long, quantity: Double, p: TobaccoTaxProfile): Double =
        when (p.specialTaxBase) {
            SpecialTaxBase.CONSUMER_PRICE -> consumerBase.toDouble()
            SpecialTaxBase.QUANTITY -> quantity
            SpecialTaxBase.SALE_PRICE -> saleBase.toDouble()
        }

    private fun calcWithheld(
        quantity: Double,
        saleBase: Long,
        consumerBase: Long,
        grossTax: Long,
        p: TobaccoTaxProfile,
    ): Long {
        if (!p.withheldTaxEnabled) return 0L
        return when (p.withheldTaxCalculationType) {
            WithheldTaxCalcType.NONE -> 0L
            WithheldTaxCalcType.FIXED_PER_UNIT ->
                ((p.withheldTaxAmount ?: 0L).toDouble() * quantity).roundToLong()
            WithheldTaxCalcType.RATE ->
                (withheldBase(saleBase, consumerBase, grossTax, p).toDouble() * (p.withheldTaxRate ?: 0) / 100.0).roundToLong()
        }
    }

    private fun withheldBase(saleBase: Long, consumerBase: Long, grossTax: Long, p: TobaccoTaxProfile): Long =
        when (p.withheldTaxBase) {
            WithheldTaxBase.CONSUMER_PRICE -> consumerBase
            WithheldTaxBase.GROSS_TAX -> grossTax
            WithheldTaxBase.SALE_PRICE -> saleBase
        }
}
