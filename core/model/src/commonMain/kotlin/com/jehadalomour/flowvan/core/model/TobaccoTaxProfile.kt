package com.jehadalomour.flowvan.core.model

/** Where the tobacco sales-tax base comes from. */
enum class TobaccoTaxBase { SALE_PRICE, CONSUMER_PRICE }

enum class SpecialTaxCalcType { NONE, FIXED_PER_UNIT, RATE, FIXED_PLUS_RATE }
enum class SpecialTaxBase { SALE_PRICE, CONSUMER_PRICE, QUANTITY }

enum class WithheldTaxCalcType { NONE, FIXED_PER_UNIT, RATE }
enum class WithheldTaxBase { SALE_PRICE, CONSUMER_PRICE, GROSS_TAX }

/**
 * A tobacco tax profile — the Kotlin twin of the ERP / cash-van
 * `tobacco_tax_profiles` row (see cash-van-dashboard/docs/SPEC-tobacco-tax.md).
 * Monetary amounts are integer **fils** per base piece; rates are plain percent
 * integers (13% = 13). Synced from the backend, attached to a tobacco item.
 */
data class TobaccoTaxProfile(
    val id: String,
    val taxBase: TobaccoTaxBase,

    val salesTaxEnabled: Boolean,
    val salesTaxRate: Int, // percent integer

    val specialTaxEnabled: Boolean,
    val specialTaxCalculationType: SpecialTaxCalcType,
    val specialTaxBase: SpecialTaxBase,
    val specialTaxRate: Int?, // percent integer
    val specialTaxFixedAmount: Long?, // fils per base piece

    val withheldTaxEnabled: Boolean,
    val withheldTaxCalculationType: WithheldTaxCalcType,
    val withheldTaxBase: WithheldTaxBase,
    val withheldTaxAmount: Long?, // fils per base piece
    val withheldTaxRate: Int?, // percent integer
)
