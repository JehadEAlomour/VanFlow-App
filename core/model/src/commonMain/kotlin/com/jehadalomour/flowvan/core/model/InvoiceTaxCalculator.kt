package com.jehadalomour.flowvan.core.model

import kotlin.math.max
import kotlin.math.min

/** Per-line (per-product) tax treatment — distinct from the global AppSettings.TaxType. */
enum class LineTaxType {
    /** Tax is added on top of the net price (prices exclude tax). */
    TAXABLE,
    /** Unit price already contains the tax; tax is extracted for reporting. */
    INCLUSIVE,
    /** No tax applied or extracted. */
    EXEMPT,
}

/** The invoice-level discount the salesman enters in the voucher footer. */
sealed class InvoiceDiscountInput {
    data object None : InvoiceDiscountInput()
    /** Percentage applied to the net after all line discounts. */
    data class Percent(val pct: Double) : InvoiceDiscountInput()
    /** Fixed JOD amount applied to the net after all line discounts. */
    data class Fixed(val amount: Double) : InvoiceDiscountInput()
}

/**
 * Full invoice calculation result per the InvoiceTaxCalculation_Spec.
 *
 * All amounts are in JOD with full double precision; round at the display / storage layer.
 */
data class VoucherSummary(
    // ── Raw totals ──────────────────────────────────────────────────────────
    val subtotalBeforeDiscounts: Double,   // Σ (qty × unitPrice)
    val totalLineDiscounts: Double,        // Σ line discount amounts
    val netAfterLineDiscounts: Double,     // Σ nets by type (= SumNetAll)

    // ── Invoice-level discount ───────────────────────────────────────────────
    val invoiceDiscountAmount: Double,

    // ── Nets by type after invoice discount ─────────────────────────────────
    val netTaxable: Double,
    val netInclusive: Double,
    val netExempt: Double,

    // ── Tax ─────────────────────────────────────────────────────────────────
    val taxOnTaxable: Double,              // added on top for TAXABLE items
    val taxInInclusive: Double,            // extracted from INCLUSIVE items (informational)
    val totalTax: Double,                  // taxOnTaxable + taxInInclusive

    // ── Grand total ─────────────────────────────────────────────────────────
    val grandTotal: Double,
) {
    companion object {
        val ZERO = VoucherSummary(
            subtotalBeforeDiscounts = 0.0,
            totalLineDiscounts = 0.0,
            netAfterLineDiscounts = 0.0,
            invoiceDiscountAmount = 0.0,
            netTaxable = 0.0,
            netInclusive = 0.0,
            netExempt = 0.0,
            taxOnTaxable = 0.0,
            taxInInclusive = 0.0,
            totalTax = 0.0,
            grandTotal = 0.0,
        )
    }
}

/**
 * Stateless calculator implementing the full tax spec.
 *
 * Input: the cart (each line carries its own taxRate and lineTaxType) plus an optional
 * invoice-level discount. Output: VoucherSummary ready for display and storage.
 */
object InvoiceTaxCalculator {

    fun calculateInvoice(
        cart: List<CartLine>,
        invoiceDiscount: InvoiceDiscountInput = InvoiceDiscountInput.None,
    ): VoucherSummary {
        if (cart.isEmpty()) return VoucherSummary.ZERO

        // ── Step 1: line totals ──────────────────────────────────────────────
        val subtotal     = cart.sumOf { it.grossLineTotal }
        val lineDiscounts = cart.sumOf { it.lineDiscount }

        // ── Step 2: group nets by tax type ───────────────────────────────────
        val sumNetTaxable   = cart.sumNetByType(LineTaxType.TAXABLE)
        val sumNetInclusive = cart.sumNetByType(LineTaxType.INCLUSIVE)
        val sumNetExempt    = cart.sumNetByType(LineTaxType.EXEMPT)
        val sumNetAll       = sumNetTaxable + sumNetInclusive + sumNetExempt

        // ── Step 3: invoice-level discount ───────────────────────────────────
        val invDisc = computeDiscount(sumNetAll, invoiceDiscount)

        // ── Step 4: distribute discount proportionally by type ───────────────
        val discTaxable   = proportional(invDisc, sumNetTaxable,   sumNetAll)
        val discInclusive = proportional(invDisc, sumNetInclusive,  sumNetAll)
        val discExempt    = proportional(invDisc, sumNetExempt,     sumNetAll)

        val finalTaxable   = sumNetTaxable   - discTaxable
        val finalInclusive = sumNetInclusive - discInclusive
        val finalExempt    = sumNetExempt    - discExempt

        // ── Step 5: re-calculate tax per line after invoice discount ─────────
        val taxOnTaxable = if (sumNetTaxable > 0.0) {
            cart.filter { it.lineTaxType == LineTaxType.TAXABLE }.sumOf { line ->
                val lineNetFinal = line.lineNet * (finalTaxable / sumNetTaxable)
                lineNetFinal * line.taxRate
            }
        } else 0.0

        val taxInInclusive = if (sumNetInclusive > 0.0) {
            cart.filter { it.lineTaxType == LineTaxType.INCLUSIVE }.sumOf { line ->
                val lineAmtFinal = line.lineNet * (finalInclusive / sumNetInclusive)
                lineAmtFinal * (line.taxRate / (1.0 + line.taxRate))
            }
        } else 0.0

        val totalTax   = taxOnTaxable + taxInInclusive
        // TAXABLE: customer pays net + tax; INCLUSIVE: tax already inside; EXEMPT: no tax
        val grandTotal = finalTaxable + taxOnTaxable + finalInclusive + finalExempt

        return VoucherSummary(
            subtotalBeforeDiscounts = subtotal,
            totalLineDiscounts      = lineDiscounts,
            netAfterLineDiscounts   = sumNetAll,
            invoiceDiscountAmount   = invDisc,
            netTaxable              = finalTaxable,
            netInclusive            = finalInclusive,
            netExempt               = finalExempt,
            taxOnTaxable            = taxOnTaxable,
            taxInInclusive          = taxInInclusive,
            totalTax                = totalTax,
            grandTotal              = grandTotal,
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun computeDiscount(base: Double, input: InvoiceDiscountInput): Double {
        val raw = when (input) {
            is InvoiceDiscountInput.None    -> 0.0
            is InvoiceDiscountInput.Percent -> base * (input.pct / 100.0)
            is InvoiceDiscountInput.Fixed   -> input.amount
        }
        return max(0.0, min(raw, base))
    }

    private fun proportional(total: Double, part: Double, whole: Double): Double =
        if (whole > 0.0) total * (part / whole) else 0.0

    private fun List<CartLine>.sumNetByType(type: LineTaxType): Double =
        filter { it.lineTaxType == type }.sumOf { it.lineNet }
}
