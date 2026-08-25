package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
import com.jehadalomour.flowvan.core.model.Customer

sealed interface StatementEntry {
    val createdAt: Long
    val amount: Double
    val number: String

    data class Invoice(val entity: InvoiceEntity) : StatementEntry {
        override val createdAt get() = entity.createdAt
        override val amount get() = entity.total
        override val number get() = entity.number
    }

    data class Payment(val entity: PaymentEntity) : StatementEntry {
        override val createdAt get() = entity.createdAt
        override val amount get() = entity.amount
        override val number get() = entity.number
    }
}

/**
 * One movement, plus the balance as it stood immediately after it.
 *
 * The running balance is per line and not only at the foot because the argument
 * in the shop doorway is never about the total — it is about one invoice, and
 * the disputed figure has to sit next to the disputed document.
 */
data class StatementLine(
    val entry: StatementEntry,
    val balanceAfter: Double,
) {
    /** True when this line reduced what the customer owes (payment or return). */
    val isCredit: Boolean get() = when (entry) {
        is StatementEntry.Payment -> true
        is StatementEntry.Invoice -> CustomerStatement.isCredit(entry.entity)
    }

    val key: String get() = when (entry) {
        is StatementEntry.Invoice -> "inv-${entry.entity.id}"
        is StatementEntry.Payment -> "pay-${entry.entity.id}"
    }
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
) {
    private val entries: List<StatementEntry> get() = lines.map { it.entry }

    // Summed through [CustomerStatement] so the foot of the page agrees with the
    // running balance beside each line — see CustomerStatement.movement.
    val totalDebits: Double get() = entries
        .filterIsInstance<StatementEntry.Invoice>()
        .sumOf { CustomerStatement.movement(it.entity).coerceAtLeast(0.0) }

    val totalCredits: Double get() {
        val payments = entries.filterIsInstance<StatementEntry.Payment>().sumOf { it.amount }
        val returns = entries.filterIsInstance<StatementEntry.Invoice>()
            .sumOf { -CustomerStatement.movement(it.entity).coerceAtMost(0.0) }
        return payments + returns
    }

    val net: Double get() = totalDebits - totalCredits

    /** What is owed at the end of the period — the number the screen exists for. */
    val closingBalance: Double get() = openingBalance + net
}

sealed interface AccountStatementEvent {
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : AccountStatementEvent
}
