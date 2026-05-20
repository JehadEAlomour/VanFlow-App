package com.jehadalomour.flowvan.shared.presentation.feature.voucherreport

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity

enum class VoucherTypeFilter { ALL, SALE, RETURN, REQUEST }
enum class VoucherKindFilter { ALL, CASH, CHEQUE, TRANSFER, CREDIT }

data class VoucherReportState(
    val customerId: String = "",
    val invoices: List<InvoiceEntity> = emptyList(),
    val typeFilter: VoucherTypeFilter = VoucherTypeFilter.ALL,
    val kindFilter: VoucherKindFilter = VoucherKindFilter.ALL,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
) {
    val total: Double get() = invoices.sumOf { it.total }
}

sealed interface VoucherReportEvent {
    data class TypeFilterChanged(val filter: VoucherTypeFilter) : VoucherReportEvent
    data class KindFilterChanged(val filter: VoucherKindFilter) : VoucherReportEvent
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : VoucherReportEvent
}
