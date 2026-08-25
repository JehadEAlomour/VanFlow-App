package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.database.entity.ShiftEntity
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.DailyKpi
import com.jehadalomour.flowvan.core.model.User

data class HomeState(
    val user: User? = null,
    val kpi: DailyKpi? = null,
    val routeTopFive: List<Customer> = emptyList(),
    val isLoading: Boolean = true,
    val activeShift: ShiftEntity? = null,
    val pendingPings: Int = 0,
    val lastSyncAt: Long? = null,
    /** Gates the Find Customers tile — permissions.canFindCustomers, read from the session. */
    val canFindCustomers: Boolean = false,
    /** Route-only: hide the Customers tile so the rep works from the route only. */
    val routesOnly: Boolean = false,
    /** Unread notifications, for the home bell badge. */
    val unreadNotifications: Int = 0,
    /** The rep's own balance from the ERP ("cash with salesman"), JOD major units. Null = unknown/unavailable. */
    val erpBalance: Double? = null,
    /** True when [erpBalance] is a real ERP figure (rep is ERP-linked and ERP is on). */
    val erpBalanceAvailable: Boolean = false,
    /** Epoch-ms the ERP balance was last fetched — the "as of" time shown when offline. 0 = never. */
    val erpBalanceAsOfMillis: Long = 0L,
)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object StartShift : HomeEvent
}
