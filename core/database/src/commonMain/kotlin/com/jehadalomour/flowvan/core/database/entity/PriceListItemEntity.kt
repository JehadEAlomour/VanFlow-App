package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity

/**
 * One product's price under a price list (from GET /price-lists/full). Cached so a
 * rep selling to a customer assigned to this list sees the contracted price offline.
 * [unitPrice] is the base-unit price in JOD; [sku] matches [ProductEntity.sku].
 */
@Entity(tableName = "price_list_items", primaryKeys = ["priceListId", "sku"])
data class PriceListItemEntity(
    val priceListId: String,
    val sku: String,
    val unitPrice: Double,
)
