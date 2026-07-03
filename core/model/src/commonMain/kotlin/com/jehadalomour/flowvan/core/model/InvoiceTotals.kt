package com.jehadalomour.flowvan.core.model

/**
 * Rolled-up money figures for a sale voucher, produced by
 * `CalculateInvoiceTotalsUseCase` from the cart lines and header discount.
 *
 * All values are JOD major units (the app converts fils → JOD at the mapper).
 */
data class InvoiceTotals(
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
) {
    companion object {
        /** Neutral value for an empty cart / initial state. */
        val EMPTY = InvoiceTotals(
            subtotal = 0.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            total = 0.0,
        )
    }
}
