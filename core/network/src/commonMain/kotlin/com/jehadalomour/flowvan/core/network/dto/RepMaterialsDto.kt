package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/** The signed-in rep's materials, grouped by warehouse. Mirrors the backend. */
@Serializable
data class RepMaterialsDto(
    val repId: String = "",
    val repName: String? = null,
    val vanStore: String? = null,
    val warehouses: List<WarehouseMaterialsDto> = emptyList(),
)

@Serializable
data class WarehouseMaterialsDto(
    val whNumber: String,
    val whName: String? = null,
    val isVan: Boolean = false,
    /** True for the rep's own van store — shown first, badged. */
    val isRepVan: Boolean = false,
    val itemCount: Int = 0,
    val totalQty: Double = 0.0,
    val items: List<WarehouseMaterialItemDto> = emptyList(),
)

@Serializable
data class WarehouseMaterialItemDto(
    val itemNumber: String,
    val itemName: String? = null,
    val qty: Double = 0.0,
)
