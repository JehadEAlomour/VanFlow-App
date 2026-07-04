package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.model.Customer

enum class CustomerTab { SUMMARY, SALES, RETURNS, REQUESTS, COLLECTIONS }

/** Dialog shown when the salesman leaves a customer. */
enum class LeaveDialog { NONE, REASON, CONFIRM }

/**
 * Why a location-locked rep is blocked from acting on this customer.
 * NONE = allowed (in range, or the rep isn't location-locked).
 */
enum class ProximityBlock { NONE, NO_GPS, TOO_FAR }

data class CustomerDashboardState(
    val customer: Customer? = null,
    val sales: List<InvoiceEntity> = emptyList(),
    val returns: List<InvoiceEntity> = emptyList(),
    val requests: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val selectedTab: CustomerTab = CustomerTab.SUMMARY,
    val isLoading: Boolean = true,
    /** Permission: require a written reason when leaving a customer with no transaction. */
    val requireVisitReason: Boolean = false,
    val leaveDialog: LeaveDialog = LeaveDialog.NONE,
    /** Set once the visit has been recorded so the screen can pop back. */
    val navigateBack: Boolean = false,
    /** Rep is location-locked (customers.requireProximity) — actions are geofenced. */
    val locationLocked: Boolean = false,
    /** When location-locked, why the customer actions are blocked (NONE = allowed). */
    val proximityBlock: ProximityBlock = ProximityBlock.NONE,
) {
    val salesTotal: Double get() = sales.sumOf { it.total }
    val returnsTotal: Double get() = returns.sumOf { it.total }
    val collectionsTotal: Double get() = payments.filter { it.status == "CONFIRMED" }.sumOf { it.amount }

    /** Customer actions (sale/return/request/collection) are permitted right now. */
    val actionsEnabled: Boolean get() = !locationLocked || proximityBlock == ProximityBlock.NONE
}

sealed interface CustomerDashboardEvent {
    data class TabSelected(val tab: CustomerTab) : CustomerDashboardEvent
    /** Back pressed; `hadTransaction` = the salesman started a sale/return/etc. this visit. */
    data class LeaveRequested(val hadTransaction: Boolean) : CustomerDashboardEvent
    data class ConfirmLeave(val reason: String?) : CustomerDashboardEvent
    data object DismissLeave : CustomerDashboardEvent
}
