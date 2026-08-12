package com.jehadalomour.flowvan.core.domain.ledger

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity

/**
 * What belongs on a customer's account statement, and what it does to the balance.
 *
 * A statement is a receivable ledger — a claim on money. Only movement that created
 * or settled debt goes on it: sales made on account, returns against them, and the
 * receipts that paid them (cash, cheque, transfer). Everything else is a document the
 * customer may well want to see, but not on the paper that says what is owed.
 *
 * These rules live here because the screen and the printed paper each had their own
 * copy, and they had already drifted: an order moved the balance by nothing on screen
 * and by its full value on paper. A statement whose two renderings disagree about the
 * closing figure is worse than one that is simply wrong. This is also the rule the
 * office dashboard applies (`features/customers/statement.ts`), so all three agree.
 */
object CustomerStatement {

    /**
     * Whether this voucher moved the customer's account at all.
     *
     * Three exclusions, in the order they bite:
     *
     *  - **Orders.** An order is a promise of goods, nothing more: nothing has been
     *    delivered and nothing is owed. It becomes a receivable when it is turned into
     *    a sale, and counting it before that bills the customer twice.
     *  - **Cancelled or rejected.** A voucher the office threw out never happened.
     *  - **Settled at the counter.** A cash, cheque or transfer sale was paid on the
     *    spot and never became a receivable. Listing it would inflate the debit total
     *    and hand the customer a demand for money they already gave you. A *null*
     *    method is kept: those rows predate the column, and hiding them would silently
     *    drop real debt.
     */
    fun isLedgerEntry(inv: InvoiceEntity): Boolean {
        if (inv.type == TYPE_ORDER) return false
        if (inv.status == STATUS_CANCELLED || inv.status == STATUS_REJECTED) return false
        val method = inv.paymentMethod
        val settledAtCounter = (inv.type == TYPE_SALE || inv.type == TYPE_RETURN) &&
            method != null && method != METHOD_CREDIT
        return !settledAtCounter
    }

    /**
     * Signed effect on what the customer owes: a sale adds to it, a return takes off.
     *
     * Anything unrecognised moves nothing. That default is deliberate — the running
     * balance beside each line and the totals at the foot are computed from this one
     * function, so an unknown voucher type cannot make the column stop adding up. A
     * shopkeeper who totals the page and lands somewhere other than the closing figure
     * stops trusting the paper.
     */
    fun movement(inv: InvoiceEntity): Double = when (inv.type) {
        TYPE_SALE -> inv.total
        TYPE_RETURN -> -inv.total
        else -> 0.0
    }

    /** True when the voucher reduced the debt, i.e. belongs in the credit column. */
    fun isCredit(inv: InvoiceEntity): Boolean = inv.type == TYPE_RETURN

    const val TYPE_SALE = "SALE"
    const val TYPE_RETURN = "RETURN"

    /** Orders are stored under the legacy name; see CreateRequestVoucherUseCase. */
    const val TYPE_ORDER = "REQUEST"

    private const val STATUS_CANCELLED = "CANCELLED"
    private const val STATUS_REJECTED = "REJECTED"
    private const val METHOD_CREDIT = "CREDIT"
}
