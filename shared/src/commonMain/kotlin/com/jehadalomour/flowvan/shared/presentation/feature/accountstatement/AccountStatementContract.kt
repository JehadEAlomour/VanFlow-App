package com.jehadalomour.flowvan.shared.presentation.feature.accountstatement

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.domain.model.Customer

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

data class AccountStatementState(
    val customer: Customer? = null,
    val entries: List<StatementEntry> = emptyList(),
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
) {
    val totalDebits: Double get() = entries
        .filterIsInstance<StatementEntry.Invoice>()
        .filter { it.entity.type == "SALE" || it.entity.type == "REQUEST" }
        .sumOf { it.amount }

    val totalCredits: Double get() {
        val payments = entries.filterIsInstance<StatementEntry.Payment>().sumOf { it.amount }
        val returns = entries.filterIsInstance<StatementEntry.Invoice>()
            .filter { it.entity.type == "RETURN" }
            .sumOf { it.amount }
        return payments + returns
    }

    val net: Double get() = totalDebits - totalCredits
}

sealed interface AccountStatementEvent {
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : AccountStatementEvent
}
