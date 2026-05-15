package com.jehadalomour.flowvan.shared.presentation.feature.transactionreport

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity

enum class TxnTypeFilter { ALL, SALE, RETURN, REQUEST }

data class TransactionReportState(
    val customerId: String = "",
    val invoices: List<InvoiceEntity> = emptyList(),
    val typeFilter: TxnTypeFilter = TxnTypeFilter.ALL,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
) {
    val total: Double get() = invoices.sumOf { it.total }
    val salesTotal: Double get() = invoices.filter { it.type == "SALE" }.sumOf { it.total }
    val returnsTotal: Double get() = invoices.filter { it.type == "RETURN" }.sumOf { it.total }
}

sealed interface TransactionReportEvent {
    data class TypeFilterChanged(val filter: TxnTypeFilter) : TransactionReportEvent
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : TransactionReportEvent
}
