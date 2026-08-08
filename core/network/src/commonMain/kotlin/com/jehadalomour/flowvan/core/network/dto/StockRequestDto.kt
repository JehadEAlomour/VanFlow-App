package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Van stock requests: asking the warehouse to load goods onto this van.
 *
 * Mirror of the backend's `src/modules/stock-requests/dto/`. Quantities travel
 * twice — `qtyOfUnit` is what the rep typed in the unit they chose, `baseQty` is
 * the same amount in stock-pool units. The manager grants in POOL units, which
 * is why `approvedBaseQty` is not comparable to `qtyOfUnit`.
 */

@Serializable
data class StockRequestLineRequest(
    val itemNumber: String,
    /** The variant unit's stock pool, or "" for the item's base pieces. */
    val stockUnitCode: String? = null,
    val qtyOfUnit: Double,
    /** Pieces per chosen unit. Sent so the figure the rep saw is the one recorded. */
    val unitBaseQty: Int? = null,
    /**
     * The item_units row picked. Required for a variant unit that owns its own
     * stock pool — without it the goods are received into the item's base pool
     * and the rep's stock check still fails after the load.
     */
    val itemUnitId: String? = null,
    val unitName: String? = null,
)

@Serializable
data class CreateStockRequestBody(
    val items: List<StockRequestLineRequest>,
    val note: String? = null,
)

@Serializable
data class StockRequestLineDto(
    val id: String,
    val itemNumber: String,
    val itemName: String,
    val stockUnitCode: String = "",
    val itemUnitId: String? = null,
    val unitName: String? = null,
    val unitBaseQty: Int = 1,
    val qtyOfUnit: String,
    val baseQty: String,
    /** Null while pending. Zero means this line was refused, not un-reviewed. */
    val approvedBaseQty: String? = null,
    val vanQtyAtRequest: String = "0",
)

@Serializable
data class StockRequestDto(
    val id: String,
    val requestNumber: String,
    /** pending | approved | rejected | cancelled | received */
    val status: String,
    val vanStoreNumber: String,
    val sourceStoreNumber: String? = null,
    val note: String? = null,
    val decisionNote: String? = null,
    val transferVoucherNumber: String? = null,
    val createdAt: String,
    val decidedAt: String? = null,
    val receivedAt: String? = null,
    val items: List<StockRequestLineDto> = emptyList(),
)
