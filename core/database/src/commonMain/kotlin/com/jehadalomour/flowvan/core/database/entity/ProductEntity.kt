package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val nameAr: String,
    val nameEn: String,
    val category: String,
    val unit: String,
    val salePrice: Double,
    val costPrice: Double,
    val vanStock: Int,
    val minStock: Int,
    val expiryDate: Long?,
    val brand: String?,
    @ColumnInfo(defaultValue = "0.16") val taxRate: Double = 0.16,
)