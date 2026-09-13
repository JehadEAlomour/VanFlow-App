package com.jehadalomour.flowvan.core.model.ledger

/**
 * A cash sale is ONE transaction, and the statement should say so.
 *
 * The ERP books a counter sale as two postings — the invoice, and the receipt that
 * settles it — and labels the receipt with the INVOICE's own number ("Receipt on
 * INV-123"). Rendered literally that is two rows carrying the same reference, a debit
 * cancelled by a credit, for money that never became a debt. The shopkeeper reads it
 * as being billed and then credited, and asks why.
 *
 * So a receipt that names a sale is shown ON THAT SALE'S LINE: one row, carrying what
 * it cost and what was paid against it.
 *
 * Everything else is the ERP's statement unchanged — the rows it sends, in its order,
 * with its own types and figures. The fold moves no money: a row's effect on the
 * balance is debit - credit either way, so the closing figure is identical whether the
 * receipt sits on its own line or on the sale's.
 *
 * A receipt that names NO sale on the statement — a payment on account, or one against
 * an invoice from an earlier period — keeps its own row. There is nothing to fold it
 * into, and inventing a home for it would move money between lines.
 */
object CashVoucherCollapse {

    /** Epoch millis → the local day it falls on, for the same-day test. */
    private const val DAY_MILLIS = 86_400_000L

    fun apply(movements: List<StatementMovement>): List<StatementMovement> {
        if (movements.isEmpty()) return movements

        // Receipts that name a document, keyed by the document they name.
        val paymentsByRef = movements
            .filter { it.docType == StatementDocType.PAYMENT && it.number.isNotBlank() }
            .groupBy { it.number }
        if (paymentsByRef.isEmpty()) return movements

        val consumed = mutableSetOf<String>()
        val out = mutableListOf<StatementMovement>()

        for (m in movements) {
            // Only a debit row can absorb a receipt: that is the document being paid.
            val settling = if (m.docType != StatementDocType.PAYMENT && m.debit > 0.0) {
                paymentsByRef[m.number]?.firstOrNull { it.id !in consumed }
            } else {
                null
            }

            if (settling != null) {
                consumed += settling.id
                // One row: what it cost, and what was paid against it.
                out += m.copy(credit = m.credit + settling.credit, method = settling.method)
            } else {
                out += m
            }
        }

        // Dropped in a second pass so a receipt listed BEFORE the sale it settles is
        // still removed — the two share a posting date, and the ERP may order them
        // either way.
        return out.filterNot { it.docType == StatementDocType.PAYMENT && it.id in consumed }
    }

}
