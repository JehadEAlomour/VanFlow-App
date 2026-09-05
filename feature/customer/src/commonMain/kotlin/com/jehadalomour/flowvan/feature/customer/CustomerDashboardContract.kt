package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
import com.jehadalomour.flowvan.core.model.Customer

enum class CustomerTab { SUMMARY, SALES, RETURNS, REQUESTS, COLLECTIONS }

/** Dialog shown when the salesman leaves a customer. */
enum class LeaveDialog { NONE, REASON, CONFIRM }

/**
 * Why a location-locked rep is blocked from acting on this customer.
 * NONE = allowed (in range, or the rep isn't location-locked).
 */
enum class ProximityBlock { NONE, NO_GPS, TOO_FAR }

/** Status of the rep's "update customer location" action, for top-bar UI feedback. */
enum class LocationUpdate { IDLE, UPDATING, SUCCESS, NO_GPS, ERROR }

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
    /**
     * Per-action permissions (permissions.canCreateSale / canCreateReturn /
     * canMakeCollection). The matching tile is HIDDEN when its flag is off. Default
     * true — these actions were always allowed, so only an explicit off hides them.
     */
    val canSell: Boolean = true,
    val canReturn: Boolean = true,
    val canCollect: Boolean = true,
    /** Status of the "update customer location" button (pin icon in the top bar). */
    val locationUpdate: LocationUpdate = LocationUpdate.IDLE,
) {
    val salesTotal: Double get() = sales.sumOf { it.total }
    val returnsTotal: Double get() = returns.sumOf { it.total }
    val collectionsTotal: Double get() = payments.filter { it.status == "CONFIRMED" }.sumOf { it.amount }

    /**
     * Balance due computed from the local ledger — the SAME rule the account
     * statement uses (unsettled sales add, returns subtract, payments subtract),
     * so the customer-info figure and the statement can never disagree. A
     * customer whose statement nets to zero reads 0.0 here too, instead of a
     * stale server balance.
     */
    val ledgerBalance: Double get() {
        val ledger = (sales + returns).filter(CustomerStatement::isLedgerEntry)
        return ledger.sumOf(CustomerStatement::movement) - payments.sumOf { it.amount }
    }

    /** Customer actions (sale/return/request/collection) are permitted right now. */
    val actionsEnabled: Boolean get() = !locationLocked || proximityBlock == ProximityBlock.NONE
}

sealed interface CustomerDashboardEvent {
    data class TabSelected(val tab: CustomerTab) : CustomerDashboardEvent
    /** Back pressed; `hadTransaction` = the salesman started a sale/return/etc. this visit. */
    data class LeaveRequested(val hadTransaction: Boolean) : CustomerDashboardEvent
    data class ConfirmLeave(val reason: String?) : CustomerDashboardEvent
    data object DismissLeave : CustomerDashboardEvent
    /** Rep tapped the top-bar pin: capture GPS and MOVE this customer's location. */
    data object UpdateLocationRequested : CustomerDashboardEvent
    /** Clear the location-update status once the feedback has been shown. */
    data object DismissLocationUpdate : CustomerDashboardEvent
}
