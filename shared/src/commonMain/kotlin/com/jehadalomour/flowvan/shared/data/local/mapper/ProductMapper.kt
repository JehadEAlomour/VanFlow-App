package com.jehadalomour.flowvan.shared.data.local.mapper

import com.jehadalomour.flowvan.shared.data.local.entity.ProductEntity
import com.jehadalomour.flowvan.shared.domain.model.Product

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
    minStock = minStock,
    expiryDate = expiryDate,
    brand = brand,
)
