package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.AppSettingsEntity
import com.jehadalomour.flowvan.core.model.AppSettings
import com.jehadalomour.flowvan.core.model.AppTheme
import com.jehadalomour.flowvan.core.model.TaxType
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage

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
    companyNameAr = companyNameAr,
    companyNameEn = companyNameEn,
    companyTaxNumber = companyTaxNumber,
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
    companyNameAr = companyNameAr,
    companyNameEn = companyNameEn,
    companyTaxNumber = companyTaxNumber,
)
