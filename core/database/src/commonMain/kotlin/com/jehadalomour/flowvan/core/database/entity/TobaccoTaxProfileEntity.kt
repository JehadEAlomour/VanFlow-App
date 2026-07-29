package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached tobacco tax profile (from GET /tobacco-tax-profiles) for offline sale tax. */
@Entity(tableName = "tobacco_tax_profiles")
data class TobaccoTaxProfileEntity(
    @PrimaryKey val id: String,
    val taxBase: String,
    val salesTaxEnabled: Boolean,
    val salesTaxRate: Int,
    @ColumnInfo(defaultValue = "0") val taxIncludedInConsumerPrice: Boolean = false,
    val specialTaxEnabled: Boolean,
    val specialTaxCalculationType: String,
    val specialTaxBase: String,
    val specialTaxRate: Int?,
    val specialTaxFixedAmount: Long?,
    val withheldTaxEnabled: Boolean,
    val withheldTaxCalculationType: String,
    val withheldTaxBase: String,
    val withheldTaxAmount: Long?,
    val withheldTaxRate: Int?,
)
