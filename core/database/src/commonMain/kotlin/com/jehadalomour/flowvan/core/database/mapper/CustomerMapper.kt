package com.jehadalomour.flowvan.shared.data.local.mapper

import com.jehadalomour.flowvan.shared.data.local.entity.CustomerEntity
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.CustomerSegment
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier

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
)