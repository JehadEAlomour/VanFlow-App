package com.jehadalomour.flowvan.shared.data.local.entity

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
)
