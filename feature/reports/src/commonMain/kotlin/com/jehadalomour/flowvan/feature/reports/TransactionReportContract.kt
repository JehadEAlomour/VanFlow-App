package com.jehadalomour.flowvan.feature.reports

import com.jehadalomour.flowvan.core.data.repository.CustomerTxn
import com.jehadalomour.flowvan.core.data.repository.TransactionReport
import com.jehadalomour.flowvan.core.data.repository.TxnKind

enum class TxnTypeFilter { ALL, SALE, RETURN, COLLECTION }

data class TransactionReportState(
    val customerId: String = "",
    val customerNumber: String = "",
    val customerNameAr: String = "",
    val report: TransactionReport = TransactionReport(),
    val typeFilter: TxnTypeFilter = TxnTypeFilter.ALL,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
    /**
     * Set when the report could not be built — no signal, or the server refused.
     * Shown INSTEAD of rows, never alongside a partial list: a transaction report
     * missing half its movement is read as fact, not as an outage.
     */
    val errorAr: String? = null,
) {
    /** The rows after the type chip, in date order. */
    val rows: List<CustomerTxn> get() = when (typeFilter) {
        TxnTypeFilter.ALL -> report.rows
        TxnTypeFilter.SALE -> report.rows.filter { it.kind == TxnKind.SALE }
        TxnTypeFilter.RETURN -> report.rows.filter { it.kind == TxnKind.RETURN }
        TxnTypeFilter.COLLECTION -> report.rows.filter { it.kind == TxnKind.COLLECTION }
    }

    // Totals always describe the WHOLE period, never the filtered view. A chip is
    // a way of looking; it is not a claim about what the customer transacted, and
    // a footer that moved with it would be read as one.
    val salesTotal: Double get() = report.salesTotal
    val returnsTotal: Double get() = report.returnsTotal
    val collectionsTotal: Double get() = report.collectionsTotal
    val netTotal: Double get() = report.netTotal
    val creditTotal: Double get() = report.creditTotal
    val cashTotal: Double get() = report.cashTotal
}

sealed interface TransactionReportEvent {
    data class TypeFilterChanged(val filter: TxnTypeFilter) : TransactionReportEvent
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : TransactionReportEvent
    data object Retry : TransactionReportEvent
}
