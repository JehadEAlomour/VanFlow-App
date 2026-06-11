package com.jehadalomour.flowvan.shared.data.local.mapper

import com.jehadalomour.flowvan.shared.data.local.entity.AppSettingsEntity
import com.jehadalomour.flowvan.shared.domain.model.AppSettings
import com.jehadalomour.flowvan.shared.domain.model.AppTheme
import com.jehadalomour.flowvan.shared.domain.model.TaxType
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage

fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
    theme = runCatching { AppTheme.valueOf(theme) }.getOrDefault(AppTheme.SYSTEM),
    language = runCatching { AppLanguage.valueOf(language) }.getOrDefault(AppLanguage.AR),
    taxType = runCatching { TaxType.valueOf(taxType) }.getOrDefault(TaxType.EXCLUDED_TAX),
    ipAddress = ipAddress,
    salesmanNumber = salesmanNumber,
    maxSaleVoucherNumber = maxSaleVoucherNumber,
    maxReturnVoucherNumber = maxReturnVoucherNumber,
    maxOrderVoucherNumber = maxOrderVoucherNumber,
    branch = branch,
    canEditPrice = canEditPrice,
    offlineModeEnabled = offlineModeEnabled,
)

fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    id = 1,
    theme = theme.name,
    language = language.name,
    taxType = taxType.name,
    ipAddress = ipAddress,
    salesmanNumber = salesmanNumber,
    maxSaleVoucherNumber = maxSaleVoucherNumber,
    maxReturnVoucherNumber = maxReturnVoucherNumber,
    maxOrderVoucherNumber = maxOrderVoucherNumber,
    branch = branch,
    canEditPrice = canEditPrice,
    offlineModeEnabled = offlineModeEnabled,
)
