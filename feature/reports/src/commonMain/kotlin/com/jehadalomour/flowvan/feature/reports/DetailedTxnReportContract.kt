package com.jehadalomour.flowvan.feature.reports

import com.jehadalomour.flowvan.core.data.repository.DetailedTxnReport

data class DetailedTxnReportState(
    val customerId: String = "",
    val customerNumber: String = "",
    val customerNameAr: String = "",
    val report: DetailedTxnReport = DetailedTxnReport(),
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
    val errorAr: String? = null,
    /** Voucher ids the user has expanded. Collapsed by default — a month of
     *  vouchers with every line open is a wall nobody reads. */
    val expanded: Set<String> = emptySet(),
)

sealed interface DetailedTxnReportEvent {
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : DetailedTxnReportEvent
    data class ToggleExpanded(val docId: String) : DetailedTxnReportEvent
    data object ExpandAll : DetailedTxnReportEvent
    data object CollapseAll : DetailedTxnReportEvent
    data object Retry : DetailedTxnReportEvent
}
