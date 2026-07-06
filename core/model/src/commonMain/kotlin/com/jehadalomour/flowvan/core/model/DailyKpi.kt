package com.jehadalomour.flowvan.core.model

data class DailyKpi(
    /** All SALE vouchers today (cash + credit). */
    val salesTotal: Double,
    /** Cash SALE vouchers only (CASH/CHEQUE/TRANSFER) — the part that counts as day cash. */
    val cashSalesTotal: Double,
    /** Credit (on-account, آجل) SALE vouchers — receivables, NOT day cash. */
    val creditSalesTotal: Double,
    val returnsTotal: Double,
    val collectionsTotal: Double,
    val customersVisited: Int,
    val customersPlanned: Int,
)
