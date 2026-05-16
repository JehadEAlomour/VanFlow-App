package com.jehadalomour.flowvan.shared.presentation.feature.settings

import com.jehadalomour.flowvan.shared.domain.model.AppTheme
import com.jehadalomour.flowvan.shared.domain.model.TaxType
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage

data class SettingsState(
    val isLoading: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.AR,
    val taxType: TaxType = TaxType.EXCLUDED_TAX,
    val ipAddress: String = "",
    val salesmanNumber: String = "",
    val maxSaleVoucherNumber: String = "9999",
    val maxReturnVoucherNumber: String = "9999",
    val maxOrderVoucherNumber: String = "9999",
    val branch: String = "",
    val canEditPrice: Boolean = false,
    val offlineModeEnabled: Boolean = false,
    val saved: Boolean = false,
)

sealed interface SettingsEvent {
    data class ThemeChanged(val theme: AppTheme) : SettingsEvent
    data class LanguageChanged(val language: AppLanguage) : SettingsEvent
    data class TaxTypeChanged(val taxType: TaxType) : SettingsEvent
    data class IpAddressChanged(val ip: String) : SettingsEvent
    data class SalesmanNumberChanged(val number: String) : SettingsEvent
    data class MaxSaleVoucherChanged(val value: String) : SettingsEvent
    data class MaxReturnVoucherChanged(val value: String) : SettingsEvent
    data class MaxOrderVoucherChanged(val value: String) : SettingsEvent
    data class BranchChanged(val branch: String) : SettingsEvent
    data class CanEditPriceChanged(val enabled: Boolean) : SettingsEvent
    data class OfflineModeChanged(val enabled: Boolean) : SettingsEvent
    data object Save : SettingsEvent
    data object DismissSaved : SettingsEvent
}
