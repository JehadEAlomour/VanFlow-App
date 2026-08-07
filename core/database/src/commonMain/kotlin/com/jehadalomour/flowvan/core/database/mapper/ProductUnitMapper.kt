package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.ProductUnitEntity
import com.jehadalomour.flowvan.core.model.ProductUnit

fun ProductUnitEntity.toDomain() = ProductUnit(
    id = id,
    productId = productId,
    name = name,
    price = price,
    conversionQty = conversionQty,
    vanStock = vanStock,
    code = code,
    isBase = isBase,
    isStockUnit = isStockUnit,
)

fun ProductUnit.toEntity() = ProductUnitEntity(
    id = id,
    productId = productId,
    name = name,
    price = price,
    conversionQty = conversionQty,
    vanStock = vanStock,
    code = code,
    isBase = isBase,
    isStockUnit = isStockUnit,
)
