package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RepKpiDto(
    val todayRevenueFils: Long = 0,
    val routeCompletionPct: Double = 0.0,
    val invoicesToday: Int = 0,
    val customersAtRisk: Int = 0,
)

@Serializable
data class VanStockItemDto(
    val productId: String,
    val sku: String = "",
    val nameAr: String = "",
    val quantity: Int = 0,
    val reorderQty: Int = 0,
    val status: String = "sufficient",   // sufficient | borderline | stockout
    val snapshotAt: String? = null,
)

@Serializable
data class LocationPingRequest(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double? = null,
    val recordedAt: String? = null,
)

@Serializable
data class LocationBulkRequest(
    val points: List<LocationPingRequest>,
)

@Serializable
data class LocationBulkResultDto(
    val accepted: Int = 0,
)

@Serializable
data class LocationPingDto(
    val id: String = "",
    val repId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracyM: Double? = null,
    val recordedAt: String? = null,
)
