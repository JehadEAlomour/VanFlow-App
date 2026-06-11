package com.jehadalomour.flowvan.shared.domain.model

data class DailyKpi(
    val salesTotal: Double,
    val returnsTotal: Double,
    val collectionsTotal: Double,
    val customersVisited: Int,
    val customersPlanned: Int,
)
