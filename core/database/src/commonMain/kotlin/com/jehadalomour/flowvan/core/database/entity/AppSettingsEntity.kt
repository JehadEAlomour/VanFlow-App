package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val theme: String = "SYSTEM",
    val language: String = "AR",
    val taxType: String = "EXCLUDED_TAX",
    val ipAddress: String = "",
    val salesmanNumber: String = "",
    val maxSaleVoucherNumber: Int = 9999,
    val maxReturnVoucherNumber: Int = 9999,
    val maxOrderVoucherNumber: Int = 9999,
    val branch: String = "",
    val canEditPrice: Boolean = false,
    val offlineModeEnabled: Boolean = false,
    // v7: company profile cached from GET /company-info, used by the printed voucher header.
    @ColumnInfo(defaultValue = "''") val companyNameAr: String = "",
    @ColumnInfo(defaultValue = "''") val companyNameEn: String = "",
    @ColumnInfo(defaultValue = "''") val companyTaxNumber: String = "",
    // v13: company logo cached from GET /company-info (a `data:<mime>;base64,...` URI),
    // stored so the printed voucher header shows the company's own logo, even offline.
    @ColumnInfo(defaultValue = "''") val companyLogo: String = "",
    // v20: company phone cached from GET /company-info, shown on the A4 document header.
    @ColumnInfo(defaultValue = "''") val companyPhone: String = "",
)
