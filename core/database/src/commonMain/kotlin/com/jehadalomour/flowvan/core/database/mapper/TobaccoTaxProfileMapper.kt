package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.TobaccoTaxProfileEntity
import com.jehadalomour.flowvan.core.model.SpecialTaxBase
import com.jehadalomour.flowvan.core.model.SpecialTaxCalcType
import com.jehadalomour.flowvan.core.model.TobaccoTaxBase
import com.jehadalomour.flowvan.core.model.TobaccoTaxProfile
import com.jehadalomour.flowvan.core.model.WithheldTaxBase
import com.jehadalomour.flowvan.core.model.WithheldTaxCalcType

private inline fun <reified T : Enum<T>> enumOr(v: String, default: T): T =
    runCatching { enumValueOf<T>(v) }.getOrDefault(default)

fun TobaccoTaxProfileEntity.toDomain(): TobaccoTaxProfile = TobaccoTaxProfile(
    id = id,
    taxBase = enumOr(taxBase, TobaccoTaxBase.CONSUMER_PRICE),
    salesTaxEnabled = salesTaxEnabled,
    salesTaxRate = salesTaxRate,
    taxIncludedInConsumerPrice = taxIncludedInConsumerPrice,
    specialTaxEnabled = specialTaxEnabled,
    specialTaxCalculationType = enumOr(specialTaxCalculationType, SpecialTaxCalcType.NONE),
    specialTaxBase = enumOr(specialTaxBase, SpecialTaxBase.QUANTITY),
    specialTaxRate = specialTaxRate,
    specialTaxFixedAmount = specialTaxFixedAmount,
    withheldTaxEnabled = withheldTaxEnabled,
    withheldTaxCalculationType = enumOr(withheldTaxCalculationType, WithheldTaxCalcType.NONE),
    withheldTaxBase = enumOr(withheldTaxBase, WithheldTaxBase.GROSS_TAX),
    withheldTaxAmount = withheldTaxAmount,
    withheldTaxRate = withheldTaxRate,
)
