package com.jehadalomour.flowvan.shared.domain.model

data class ProductUnit(
    val id: String,
    val productId: String,
    val name: String,
    val price: Double,
    val conversionQty: Double,
)
