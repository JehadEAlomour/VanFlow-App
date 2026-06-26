package com.jehadalomour.flowvan.core.model

data class Product(
    val id: String,
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
    val taxRate: Double = 0.16,
    /** Product image URL (absolute; resolved from the ERP). Null = show the avatar. */
    val imageUrl: String? = null,
)