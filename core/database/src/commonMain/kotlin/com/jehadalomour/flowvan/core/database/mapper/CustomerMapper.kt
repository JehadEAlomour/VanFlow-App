package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.CustomerSegment
import com.jehadalomour.flowvan.core.model.CustomerTier

fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    code = code,
    nameAr = nameAr,
    nameEn = nameEn,
    phone = phone,
    area = area,
    addressAr = addressAr,
    tier = runCatching { CustomerTier.valueOf(tier) }.getOrDefault(CustomerTier.C),
    segment = runCatching { CustomerSegment.valueOf(segment) }.getOrDefault(CustomerSegment.REGULAR),
    churnRisk = churnRisk,
    balance = balance,
    overdueAmount = overdueAmount,
    creditLimit = creditLimit,
    taxNumber = taxNumber,
    isOnRoute = isOnRoute,
    visitOrder = visitOrder,
    lat = lat,
    lng = lng,
    category = category,
    regionId = regionId,
    repId = repId,
    priceListId = priceListId,
)