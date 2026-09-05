package com.jehadalomour.flowvan.core.model

import com.jehadalomour.flowvan.core.common.i18n.AppLanguage

enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class TaxType { INCLUDED_TAX, EXCLUDED_TAX }

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.AR,
    val taxType: TaxType = TaxType.EXCLUDED_TAX,
    val ipAddress: String = "",
    val salesmanNumber: String = "",
    val maxSaleVoucherNumber: Int = 9999,
    val maxReturnVoucherNumber: Int = 9999,
    val maxOrderVoucherNumber: Int = 9999,
    val branch: String = "",
    val canEditPrice: Boolean = false,
    val offlineModeEnabled: Boolean = false,
    // Company profile cached from GET /company-info, used by the printed voucher header.
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    /** Company logo as a `data:<mime>;base64,...` URI (cached from /company-info) for print. */
    val companyLogo: String = "",
    /** Company phone cached from /company-info, shown on the A4 document header. */
    val companyPhone: String = "",
)
