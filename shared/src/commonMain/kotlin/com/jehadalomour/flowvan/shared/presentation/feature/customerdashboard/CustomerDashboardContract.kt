package com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.domain.model.Customer

enum class CustomerTab { SUMMARY, SALES, RETURNS, REQUESTS, COLLECTIONS }

data class CustomerDashboardState(
    val customer: Customer? = null,
    val sales: List<InvoiceEntity> = emptyList(),
    val returns: List<InvoiceEntity> = emptyList(),
    val requests: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val selectedTab: CustomerTab = CustomerTab.SUMMARY,
    val isLoading: Boolean = true,
) {
    val salesTotal: Double get() = sales.sumOf { it.total }
    val returnsTotal: Double get() = returns.sumOf { it.total }
    val collectionsTotal: Double get() = payments.filter { it.status == "CONFIRMED" }.sumOf { it.amount }
}

sealed interface CustomerDashboardEvent {
    data class TabSelected(val tab: CustomerTab) : CustomerDashboardEvent
}
