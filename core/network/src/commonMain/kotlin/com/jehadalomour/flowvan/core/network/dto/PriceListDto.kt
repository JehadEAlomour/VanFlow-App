package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/** A price list + its item prices from GET /api/v1/price-lists/full. */
@Serializable
data class PriceListDto(
    val id: String,
    val code: String = "",
    val name: String = "",
    val items: List<PriceListItemDto> = emptyList(),
)

@Serializable
data class PriceListItemDto(
    val itemNumber: String? = null,
    val barcode: String? = null,
    /** Base-unit price in fils. */
    val unitPrice: Long = 0,
)
