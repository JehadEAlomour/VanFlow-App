package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.ProductEntity
import com.jehadalomour.flowvan.core.model.Product

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    sku = sku,
    nameAr = nameAr,
    nameEn = nameEn,
    category = category,
    unit = unit,
    salePrice = salePrice,
    costPrice = costPrice,
    vanStock = vanStock,
    mainStock = mainStock,
    minStock = minStock,
    expiryDate = expiryDate,
    brand = brand,
    taxRate = taxRate,
    imageUrl = imageUrl,
    isTobacco = isTobacco,
    tobaccoProfileId = tobaccoProfileId,
    consumerPriceFils = consumerPriceFils,
)
