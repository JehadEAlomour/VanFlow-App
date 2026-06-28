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
)
