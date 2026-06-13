package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.model.AppTheme
import com.jehadalomour.flowvan.core.model.TaxType
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage

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
    val apiBaseUrl: String = "",
    val isRefreshing: Boolean = false,
    val refreshMessage: String? = null,
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
    data class ApiBaseUrlChanged(val url: String) : SettingsEvent
    data object RefreshCatalog : SettingsEvent
    data object DismissRefreshMessage : SettingsEvent
    data object Save : SettingsEvent
    data object DismissSaved : SettingsEvent
}
