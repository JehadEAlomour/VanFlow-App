package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.model.Customer

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
