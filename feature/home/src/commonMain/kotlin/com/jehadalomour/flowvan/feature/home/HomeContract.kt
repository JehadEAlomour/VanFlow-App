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
)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object StartShift : HomeEvent
}
