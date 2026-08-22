package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.dto.WarehouseMaterialsDto
import com.jehadalomour.flowvan.core.network.http.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepMaterialsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val warehouses: List<WarehouseMaterialsDto> = emptyList(),
)

/** The signed-in rep's own materials, grouped by warehouse. Read-only. */
class RepMaterialsViewModel(
    private val repApi: RepApi,
) : ViewModel() {

    private val _state = MutableStateFlow(RepMaterialsState())
    val state: StateFlow<RepMaterialsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val res = repApi.myMaterialsByWarehouse()
                _state.update { it.copy(isLoading = false, warehouses = res.warehouses, error = null) }
            } catch (e: NetworkException) {
                _state.update { it.copy(isLoading = false, error = e.error.messageAr) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
