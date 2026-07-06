package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.CustomerSegment
import com.jehadalomour.flowvan.core.model.CustomerTier

data class CustomerListState(
    val all: List<Customer> = emptyList(),
    val visible: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val tierFilter: CustomerTier? = null,
    val segmentFilter: CustomerSegment? = null,
    val isLoading: Boolean = true,
    /** Whether the signed-in rep may create customers (permissions.canAddCustomer). */
    val canAddCustomer: Boolean = false,
)

sealed interface CustomerListEvent {
    data class SearchChanged(val query: String) : CustomerListEvent
    data class TierFilter(val tier: CustomerTier?) : CustomerListEvent
    data class SegmentFilter(val segment: CustomerSegment?) : CustomerListEvent
}
