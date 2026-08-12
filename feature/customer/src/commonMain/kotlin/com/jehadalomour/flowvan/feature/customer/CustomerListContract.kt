package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.model.Customer

/**
 * The four ways a rep narrows the round. Deliberately not tier or segment —
 * those were in the state and never applied to anything, because standing in a
 * doorway nobody filters by "segment". They filter by "who owes me" and "who is
 * on today's route".
 */
enum class CustomerFilter { ALL, ON_ROUTE, OWING, OVER_LIMIT }

data class CustomerListState(
    val all: List<Customer> = emptyList(),
    val visible: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val filter: CustomerFilter = CustomerFilter.ALL,
    val isLoading: Boolean = true,
    /** Whether the signed-in rep may create customers (permissions.canAddCustomer). */
    val canAddCustomer: Boolean = false,
) {
    /** A search that matched nothing is a different screen from having no customers. */
    val isSearching: Boolean get() = searchQuery.isNotBlank()
}

sealed interface CustomerListEvent {
    data class SearchChanged(val query: String) : CustomerListEvent
    data class FilterChanged(val filter: CustomerFilter) : CustomerListEvent
    data object ClearSearch : CustomerListEvent
}
