package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.database.entity.TobaccoTaxProfileEntity
import com.jehadalomour.flowvan.core.network.dto.TobaccoTaxProfileDto

fun TobaccoTaxProfileDto.toEntity(): TobaccoTaxProfileEntity = TobaccoTaxProfileEntity(
    id = id,
    taxBase = taxBase,
    salesTaxEnabled = salesTaxEnabled,
    salesTaxRate = salesTaxRate,
    taxIncludedInConsumerPrice = taxIncludedInConsumerPrice,
    specialTaxEnabled = specialTaxEnabled,
    specialTaxCalculationType = specialTaxCalculationType,
    specialTaxBase = specialTaxBase,
    specialTaxRate = specialTaxRate,
    specialTaxFixedAmount = specialTaxFixedAmount,
    withheldTaxEnabled = withheldTaxEnabled,
    withheldTaxCalculationType = withheldTaxCalculationType,
    withheldTaxBase = withheldTaxBase,
    withheldTaxAmount = withheldTaxAmount,
    withheldTaxRate = withheldTaxRate,
)
