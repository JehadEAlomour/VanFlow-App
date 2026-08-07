package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RepKpiDto(
    val todayRevenueFils: Long = 0,
    val routeCompletionPct: Double = 0.0,
    val invoicesToday: Int = 0,
    val customersAtRisk: Int = 0,
)

/**
 * One van-stock POOL. The feed is per (product, stock unit): `stockUnitCode = ""` is the
 * item's base pool and keeps the old shape, so an old APK reading productId/quantity still
 * sees exactly what it saw before; a variant unit adds its own row.
 */
@Serializable
data class VanStockItemDto(
    val productId: String,
    val sku: String = "",
    val nameAr: String = "",
    val quantity: Int = 0,
    val reorderQty: Int = 0,
    val status: String = "sufficient",   // sufficient | borderline | stockout
    val snapshotAt: String? = null,
    /** "" = the item's base pool; otherwise the variant unit's code. */
    val stockUnitCode: String? = "",
    /**
     * `item_units.id` of the variant pool; NULL on a base-pool row.
     *
     * Nullable, not `String = ""`: a default only covers a MISSING key, so an explicit
     * `"itemUnitId": null` — which is exactly what the server sends for the base pool —
     * throws "Unexpected null value for non-nullable field" and takes the whole van-stock
     * call down with it. That failure is silent in the UI: the catalog still refreshes
     * (it runs first) and every stock number stays 0.
     */
    val itemUnitId: String? = null,
    val unitName: String? = null,
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
data class HeartbeatRequest(
    val gpsEnabled: Boolean,
    val appState: String = "active",   // "active" | "signed_out"
    val batteryPct: Int? = null,
)

@Serializable
data class HeartbeatResultDto(
    val ok: Boolean = true,
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
