package com.jehadalomour.flowvan.core.model.ledger


/**
 * A cash sale is ONE transaction, and the statement should say so.
 *
 * The ERP books a counter sale as two postings — the invoice, and the receipt that
 * settles it the same moment — and labels the receipt with the INVOICE's own number
 * ("Receipt on INV-123"). Rendered literally that is two rows carrying the same
 * reference, a debit immediately cancelled by a credit, for money that never became
 * a debt. The shopkeeper reads it as being billed and then credited, and asks why.
 *
 * So the pair is folded into the single row it describes: the sale, showing both what
 * it cost and that it was paid, moving the balance by nothing.
 *
 * WHAT IS DELIBERATELY NOT FOLDED
 *
 * Sharing a reference is not enough. A receipt against a CREDIT invoice names that
 * invoice too, and it is a different event on a different day — the shop needs to see
 * when they paid, and folding it would erase the payment from their history. So all
 * three must hold:
 *
 *   - same reference — the receipt is against that invoice;
 *   - same day — a counter sale is settled where it is made, not next Tuesday;
 *   - settled in full — a part payment leaves a real balance and a real row.
 *
 * Everything else passes through untouched, and the running balance is unaffected
 * either way: a folded pair nets to zero exactly as the two rows did.
 */
object CashVoucherCollapse {

    /** Epoch millis → the local day it falls on, for the same-day test. */
    private const val DAY_MILLIS = 86_400_000L

    fun apply(movements: List<StatementMovement>): List<StatementMovement> {
        if (movements.isEmpty()) return movements

        // Receipts that could settle a sale, keyed by the document they name.
        val paymentsByRef = movements
            .filter { it.docType == StatementDocType.PAYMENT && it.number.isNotBlank() }
            .groupBy { it.number }
        if (paymentsByRef.isEmpty()) return movements

        val consumed = mutableSetOf<String>()
        val out = mutableListOf<StatementMovement>()

        for (m in movements) {
            val settling = if (m.docType == StatementDocType.SALE && m.debit > 0.0) {
                paymentsByRef[m.number]?.firstOrNull { p ->
                    p.id !in consumed &&
                        sameDay(p.createdAt, m.createdAt) &&
                        settlesInFull(p.credit, m.debit)
                }
            } else {
                null
            }

            if (settling != null) {
                consumed += settling.id
                // One row: what it cost, and that it was paid. movement = 0.
                out += m.copy(credit = settling.credit, method = settling.method)
            } else {
                out += m
            }
        }

        // Second pass drops the receipts that were folded in. Done here rather than
        // inline so a receipt appearing BEFORE its invoice in the list is still
        // removed — the ERP orders by posting date, and the two share one.
        return out.filterNot { it.docType == StatementDocType.PAYMENT && it.id in consumed }
    }

    private fun sameDay(a: Long, b: Long): Boolean = a / DAY_MILLIS == b / DAY_MILLIS

    /**
     * Equal to the last fils. Money here is major units carried as Double, so an
     * exact == would fail on a value that survived a division; half a fils is far
     * below anything a statement can render and far above float noise.
     */
    private fun settlesInFull(paid: Double, owed: Double): Boolean =
        kotlin.math.abs(paid - owed) < 0.0005
}
