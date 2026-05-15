package com.jehadalomour.flowvan.shared.presentation.feature.paymentreport

import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity

enum class PaymentMethodFilter { ALL, CASH, CHEQUE, TRANSFER }

data class PaymentReportState(
    val customerId: String = "",
    val payments: List<PaymentEntity> = emptyList(),
    val methodFilter: PaymentMethodFilter = PaymentMethodFilter.ALL,
    val fromMillis: Long = 0L,
    val toMillis: Long = 0L,
    val isLoading: Boolean = true,
) {
    val total: Double get() = payments.sumOf { it.amount }
    val confirmedTotal: Double get() = payments.filter { it.status == "CONFIRMED" }.sumOf { it.amount }
}

sealed interface PaymentReportEvent {
    data class MethodFilterChanged(val filter: PaymentMethodFilter) : PaymentReportEvent
    data class DateRangeChanged(val fromMillis: Long, val toMillis: Long) : PaymentReportEvent
}
