package com.jehadalomour.flowvan.core.model

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

    /**
     * Delegates to the canonical fils engine [VoucherCalc] (the backend's twin),
     * then re-projects the integer-fils result into the [VoucherSummary] the UI
     * expects. All lines share one tax mode (from the global setting); EXEMPT and
     * zero-rate lines simply carry rate 0.
     */
    fun calculateInvoice(
        cart: List<CartLine>,
        invoiceDiscount: InvoiceDiscountInput = InvoiceDiscountInput.None,
    ): VoucherSummary {
        if (cart.isEmpty()) return VoucherSummary.ZERO

        val mode =
            if (cart.any { it.lineTaxType == LineTaxType.INCLUSIVE }) TaxMode.INCLUSIVE
            else TaxMode.EXCLUSIVE
        val headerPct = (invoiceDiscount as? InvoiceDiscountInput.Percent)?.pct ?: 0.0
        val headerFils = (invoiceDiscount as? InvoiceDiscountInput.Fixed)?.amount?.toFils() ?: 0L

        val result = VoucherCalc.calc(
            CalcInput(
                taxMode = mode,
                headerDiscountPct = headerPct,
                headerDiscountFils = headerFils,
                lines = cart.map { line ->
                    CalcLineInput(
                        unitPriceFils = line.unitPrice.toFils(),
                        qty = line.qty,
                        lineDiscountPct = line.discountPct.coerceIn(0.0, 1.0) * 100.0,
                        lineDiscountFils = 0L,
                        taxRatePct = if (line.lineTaxType == LineTaxType.EXEMPT) 0.0
                        else line.taxRate * 100.0,
                    )
                },
            ),
        )

        // Re-bucket nets/tax by type for the summary (same index as the cart).
        var netTaxable = 0L
        var netInclusive = 0L
        var netExempt = 0L
        var taxOnTaxable = 0L
        var taxInInclusive = 0L
        cart.forEachIndexed { i, line ->
            val r = result.lines[i]
            when {
                line.lineTaxType == LineTaxType.INCLUSIVE -> {
                    netInclusive += r.netFils; taxInInclusive += r.taxFils
                }
                line.lineTaxType == LineTaxType.EXEMPT || line.taxRate <= 0.0 -> {
                    netExempt += r.netFils
                }
                else -> {
                    netTaxable += r.netFils; taxOnTaxable += r.taxFils
                }
            }
        }

        return VoucherSummary(
            subtotalBeforeDiscounts = result.lines.sumOf { it.grossFils }.filsToJod(),
            totalLineDiscounts      = result.totalLineDiscountFils.filsToJod(),
            netAfterLineDiscounts   = result.lines.sumOf { it.netBeforeHeaderFils }.filsToJod(),
            invoiceDiscountAmount   = result.headerDiscountFils.filsToJod(),
            netTaxable              = netTaxable.filsToJod(),
            netInclusive            = netInclusive.filsToJod(),
            netExempt               = netExempt.filsToJod(),
            taxOnTaxable            = taxOnTaxable.filsToJod(),
            taxInInclusive          = taxInInclusive.filsToJod(),
            totalTax                = result.totalTaxFils.filsToJod(),
            grandTotal              = result.grandTotalFils.filsToJod(),
        )
    }
}
