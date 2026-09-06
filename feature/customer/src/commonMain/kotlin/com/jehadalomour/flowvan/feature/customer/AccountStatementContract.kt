package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.ledger.StatementMovement

/**
 * One movement, plus the balance as it stood immediately after it.
 *
 * The running balance is per line and not only at the foot because the argument
 * in the shop doorway is never about the total — it is about one invoice, and
 * the disputed figure has to sit next to the disputed document.
 */
data class StatementLine(
    val entry: StatementMovement,
    val balanceAfter: Double,
) {
    /** True when this line reduced what the customer owes (payment or return). */
    val isCredit: Boolean get() = entry.credit > 0.0

    val key: String get() = "${entry.docType}-${entry.id}"
}

/** Where the figures on screen came from. */
enum class StatementSource {
    /** The server — the whole account, including what other vans and the office did. */
    LIVE,

    /**
     * This device's own ledger. Only what this handset synced, so it can be short —
     * the screen says so rather than letting a partial statement pass for a complete one.
     */
    LOCAL,
}

data class AccountStatementState(
    val customer: Customer? = null,
    /** Newest first, as the list renders them. */
    val lines: List<StatementLine> = emptyList(),
    /** What was owed the moment before [fromMillis]. */
    val openingBalance: Double = 0.0,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
    val source: StatementSource = StatementSource.LIVE,
) {
    private val entries: List<StatementMovement> get() = lines.map { it.entry }

    // Summed from the same debit/credit columns the running balance walks, so the
    // foot of the page always agrees with the last line beside it.
    val totalDebits: Double get() = entries.sumOf { it.debit }

    val totalCredits: Double get() = entries.sumOf { it.credit }

    val net: Double get() = totalDebits - totalCredits

    /** What is owed at the end of the period — the number the screen exists for. */
    val closingBalance: Double get() = openingBalance + net

    /** True when the figures came off this device and may be short. */
    val isLocalOnly: Boolean get() = source == StatementSource.LOCAL

}

sealed interface AccountStatementEvent {
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : AccountStatementEvent
    /** The rep asked to try the server again after a local-only load. */
    data object Retry : AccountStatementEvent
}
