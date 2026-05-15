package com.jehadalomour.flowvan.shared.presentation.feature.home

import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.DailyKpi
import com.jehadalomour.flowvan.shared.domain.model.User

data class HomeState(
    val user: User? = null,
    val kpi: DailyKpi? = null,
    val routeTopFive: List<Customer> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
}
