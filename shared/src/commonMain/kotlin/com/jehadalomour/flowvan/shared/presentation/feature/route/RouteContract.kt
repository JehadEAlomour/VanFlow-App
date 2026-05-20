package com.jehadalomour.flowvan.shared.presentation.feature.route

import com.jehadalomour.flowvan.shared.domain.model.Customer

data class RouteState(
    val routeCustomers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Customer> = emptyList(),
    val visitedCount: Int = 0,
    val plannedCount: Int = 0,
    val isLoading: Boolean = true,
)

sealed interface RouteEvent {
    data class SearchChanged(val query: String) : RouteEvent
    data object ClearSearch : RouteEvent
}
