package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_units",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("product_id")],
)
data class ProductUnitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "conversion_qty") val conversionQty: Double,
    /**
     * Van stock for THIS unit's own pool, in base pieces. Only meaningful when
     * [isStockUnit] — a packaging unit (كرتونة ×12) draws from the item's base pool
     * (`products.vanStock`) and leaves this at 0.
     */
    @ColumnInfo(name = "van_stock", defaultValue = "0") val vanStock: Int = 0,
    /** The unit's code as the server knows it — the pool key on the backend. */
    @ColumnInfo(name = "code", defaultValue = "''") val code: String = "",
    /** The item's base unit (exactly one per item); the server decides, we never infer. */
    @ColumnInfo(name = "is_base", defaultValue = "0") val isBase: Boolean = false,
    /**
     * This unit is a VARIANT (its own goods, its own stock), not packaging. Defaults to
     * false so an install that upgrades and never re-syncs keeps one pool per item.
     */
    @ColumnInfo(name = "is_stock_unit", defaultValue = "0") val isStockUnit: Boolean = false,
)
