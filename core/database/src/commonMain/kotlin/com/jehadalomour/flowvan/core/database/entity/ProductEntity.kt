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
    /** Main-store on-hand for the ORDER flow — cached so orders work offline. */
    @ColumnInfo(defaultValue = "0") val mainStock: Int = 0,
    val minStock: Int,
    val expiryDate: Long?,
    val brand: String?,
    @ColumnInfo(defaultValue = "0.16") val taxRate: Double = 0.16,
    val imageUrl: String? = null,
    // ── Tobacco tax (defaults let Room auto-migrate v13→v14) ────────────────────
    @ColumnInfo(defaultValue = "0") val isTobacco: Boolean = false,
    val tobaccoProfileId: String? = null,
    @ColumnInfo(defaultValue = "0") val consumerPriceFils: Long = 0,
)