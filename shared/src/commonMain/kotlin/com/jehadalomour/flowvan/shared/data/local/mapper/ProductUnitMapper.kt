package com.jehadalomour.flowvan.shared.data.local.mapper

import com.jehadalomour.flowvan.shared.data.local.entity.ProductUnitEntity
import com.jehadalomour.flowvan.shared.domain.model.ProductUnit

fun ProductUnitEntity.toDomain() = ProductUnit(
    id = id,
    productId = productId,
    name = name,
    price = price,
    conversionQty = conversionQty,
)

fun ProductUnit.toEntity() = ProductUnitEntity(
    id = id,
    productId = productId,
    name = name,
    price = price,
    conversionQty = conversionQty,
)
