package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/** The main depot's current stock per pool — what a van load can draw from. */
@Serializable
data class MainStoreStockDto(
    val storeNumber: String? = null,
    val storeName: String? = null,
    val items: List<MainStoreItemDto> = emptyList(),
)

@Serializable
data class MainStoreItemDto(
    val itemNumber: String,
    val stockUnitCode: String = "",
    val qty: Double = 0.0,
)
