package com.jehadalomour.flowvan.core.model.ledger

/** What produced a statement line. Orders never reach here — see CustomerStatement. */
enum class StatementDocType { SALE, RETURN, PAYMENT }

/**
 * One movement on a customer's account, with no trace of where it came from.
 *
 * The statement has two sources that answer different questions: the SERVER knows the
 * whole account — what another van sold this shop, what the office entered — and the
 * DEVICE knows only what it synced itself. Both produce this, so everything downstream
 * (the running balance, the totals, the row, the paper) is written once and cannot
 * disagree about a figure depending on which one filled it.
 */
data class StatementMovement(
    val id: String,
    val number: String,
    /** Epoch millis. A server row dates from the day it carries, at local midnight. */
    val createdAt: Long,
    val docType: StatementDocType,
    /** Adds to what the customer owes. Zero on a credit row. */
    val debit: Double = 0.0,
    /** Takes off what the customer owes. Zero on a debit row. */
    val credit: Double = 0.0,
    /** CASH / CHEQUE / TRANSFER — payments only. */
    val method: String? = null,
    /** A post-dated cheque's due date, epoch millis. */
    val chequeDate: Long? = null,
    /**
     * Whether the document behind this row is held on THIS device.
     *
     * A server row names a voucher this handset never created, so there is nothing
     * local to open: the row is shown, and tapping it does nothing rather than
     * opening a blank screen.
     */
    val isLocal: Boolean = false,
) {
    /** Signed effect on the balance: positive adds debt, negative takes it off. */
    val movement: Double get() = debit - credit
}

/**
 * A customer's account over one period: what was owed before it, and everything that
 * happened during it.
 *
 * [isLive] says the figures came off the server — the whole account. False means they
 * were rebuilt from this device, which holds only what it synced itself, and the screen
 * has to say so: a rep who shows a shopkeeper a partial statement believing it complete
 * loses the argument the moment the shopkeeper names an invoice that is not on it.
 */
data class StatementSnapshot(
    /** What was owed the instant before the period began. */
    val openingBalance: Double = 0.0,
    /** Oldest first — a running balance only means anything accumulated forwards. */
    val movements: List<StatementMovement> = emptyList(),
    val isLive: Boolean = false,
)
