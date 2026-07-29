package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Tobacco tax profile as served by GET /api/v1/tobacco-tax-profiles. Field names match
 * the backend entity; money amounts are integer fils, rates are percent integers.
 * All defaulted so `ignoreUnknownKeys` + missing keys decode safely.
 */
@Serializable
data class TobaccoTaxProfileDto(
    val id: String = "",
    val taxBase: String = "CONSUMER_PRICE",
    val salesTaxEnabled: Boolean = true,
    val salesTaxRate: Int = 0,
    val taxIncludedInConsumerPrice: Boolean = false,
    val specialTaxEnabled: Boolean = false,
    val specialTaxCalculationType: String = "NONE",
    val specialTaxBase: String = "QUANTITY",
    val specialTaxRate: Int? = null,
    val specialTaxFixedAmount: Long? = null,
    val withheldTaxEnabled: Boolean = false,
    val withheldTaxCalculationType: String = "NONE",
    val withheldTaxBase: String = "GROSS_TAX",
    val withheldTaxAmount: Long? = null,
    val withheldTaxRate: Int? = null,
    val isActive: Boolean = true,
)
