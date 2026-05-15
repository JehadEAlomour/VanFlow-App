package com.jehadalomour.flowvan.shared.presentation.feature.customers

import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.CustomerSegment
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier

data class CustomerListState(
    val all: List<Customer> = emptyList(),
    val visible: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val tierFilter: CustomerTier? = null,
    val segmentFilter: CustomerSegment? = null,
    val isLoading: Boolean = true,
)

sealed interface CustomerListEvent {
    data class SearchChanged(val query: String) : CustomerListEvent
    data class TierFilter(val tier: CustomerTier?) : CustomerListEvent
    data class SegmentFilter(val segment: CustomerSegment?) : CustomerListEvent
}
