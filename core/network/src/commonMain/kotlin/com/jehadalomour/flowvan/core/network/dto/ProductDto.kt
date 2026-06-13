package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val itemNumber: String = "",
    val sku: String = "",
    val barcode: String = "",
    val name: String = "",
    val nameAr: String = "",
    val nameEn: String? = null,
    val categoryId: String? = null,
    val unit: String = "carton",
    val unitOfMeasure: String = "PCE",
    val price: Long = 0,                 // fils
    val cost: Long? = null,              // fils
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val reorderQty: Int = 0,
    val taxType: String = "TAXABLE",
    val taxCategory: String = "S",
    val taxRate: String = "0.1600",
)

@Serializable
data class QuoteRequest(
    val qty: Double,
    val customerId: String? = null,
)

@Serializable
data class QuoteDto(
    val productId: String,
    val qty: Double,
    val segment: String? = null,
    val listUnitPrice: Long = 0,         // fils
    val appliedRuleId: String? = null,
    val discountPct: Double = 0.0,
    val finalUnitPrice: Long = 0,        // fils
    val lineTotal: Long = 0,             // fils
)
