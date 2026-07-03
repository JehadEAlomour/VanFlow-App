package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.model.AppSettings
import com.jehadalomour.flowvan.core.domain.usecase.RefreshCatalogUseCase
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.settings_enter_server_first
import com.jehadalomour.flowvan.core.designsystem.resources.settings_refresh_failed
import com.jehadalomour.flowvan.core.designsystem.resources.settings_refresh_success
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: AppSettingsRepository,
    private val apiConfig: ApiConfig,
    private val refreshCatalog: RefreshCatalogUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(apiBaseUrl = apiConfig.baseUrl))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        repo.observe()
            .onEach { settings ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        theme = settings.theme,
                        language = settings.language,
                        taxType = settings.taxType,
                        ipAddress = settings.ipAddress,
                        salesmanNumber = settings.salesmanNumber,
                        maxSaleVoucherNumber = settings.maxSaleVoucherNumber.toString(),
                        maxReturnVoucherNumber = settings.maxReturnVoucherNumber.toString(),
                        maxOrderVoucherNumber = settings.maxOrderVoucherNumber.toString(),
                        branch = settings.branch,
                        canEditPrice = settings.canEditPrice,
                        offlineModeEnabled = settings.offlineModeEnabled,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeChanged -> _state.update { it.copy(theme = event.theme) }
            is SettingsEvent.LanguageChanged -> _state.update { it.copy(language = event.language) }
            is SettingsEvent.TaxTypeChanged -> _state.update { it.copy(taxType = event.taxType) }
            is SettingsEvent.IpAddressChanged -> _state.update { it.copy(ipAddress = event.ip) }
            is SettingsEvent.SalesmanNumberChanged -> _state.update { it.copy(salesmanNumber = event.number) }
            is SettingsEvent.MaxSaleVoucherChanged -> _state.update { it.copy(maxSaleVoucherNumber = event.value) }
            is SettingsEvent.MaxReturnVoucherChanged -> _state.update { it.copy(maxReturnVoucherNumber = event.value) }
            is SettingsEvent.MaxOrderVoucherChanged -> _state.update { it.copy(maxOrderVoucherNumber = event.value) }
            is SettingsEvent.BranchChanged -> _state.update { it.copy(branch = event.branch) }
            is SettingsEvent.CanEditPriceChanged -> _state.update { it.copy(canEditPrice = event.enabled) }
            is SettingsEvent.OfflineModeChanged -> _state.update { it.copy(offlineModeEnabled = event.enabled) }
            is SettingsEvent.ApiBaseUrlChanged -> _state.update { it.copy(apiBaseUrl = event.url) }
            SettingsEvent.RefreshCatalog -> refreshCatalogNow()
            SettingsEvent.DismissRefreshMessage -> _state.update { it.copy(refreshMessage = null) }
            SettingsEvent.Save -> save()
            SettingsEvent.DismissSaved -> _state.update { it.copy(saved = false) }
        }
    }

    private fun refreshCatalogNow() {
        // Persist the URL first so the API client uses it immediately.
        apiConfig.baseUrl = _state.value.apiBaseUrl
        if (!apiConfig.isEnabled) {
            viewModelScope.launch {
                val msg = getString(Res.string.settings_enter_server_first)
                _state.update { it.copy(refreshMessage = msg) }
            }
            return
        }
        _state.update { it.copy(isRefreshing = true, refreshMessage = null) }
        viewModelScope.launch {
            val result = refreshCatalog()
            val message = result.fold(
                onSuccess = { r -> getString(Res.string.settings_refresh_success, r.customers, r.products) },
                onFailure = { getString(Res.string.settings_refresh_failed) },
            )
            _state.update { it.copy(isRefreshing = false, refreshMessage = message) }
        }
    }

    private fun save() {
        val s = _state.value
        apiConfig.baseUrl = s.apiBaseUrl
        viewModelScope.launch {
            repo.save(
                AppSettings(
                    theme = s.theme,
                    language = s.language,
                    taxType = s.taxType,
                    ipAddress = s.ipAddress,
                    salesmanNumber = s.salesmanNumber,
                    maxSaleVoucherNumber = s.maxSaleVoucherNumber.toIntOrNull() ?: 9999,
                    maxReturnVoucherNumber = s.maxReturnVoucherNumber.toIntOrNull() ?: 9999,
                    maxOrderVoucherNumber = s.maxOrderVoucherNumber.toIntOrNull() ?: 9999,
                    branch = s.branch,
                    canEditPrice = s.canEditPrice,
                    offlineModeEnabled = s.offlineModeEnabled,
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
