package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.network.api.MyRouteApi
import com.jehadalomour.flowvan.core.network.dto.MyRouteStopDto
import com.jehadalomour.flowvan.core.network.http.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayRouteState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stops: List<MyRouteStopDto> = emptyList(),
    val markingId: String? = null,
) {
    val doneCount: Int get() = stops.count { it.todoDoneToday || it.todo.isNullOrBlank() }
    val total: Int get() = stops.size
}

/** Loads the signed-in salesman's outlets for today and completes their to-dos. */
class TodayRouteViewModel(
    private val api: MyRouteApi,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayRouteState())
    val state: StateFlow<TodayRouteState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val stops = api.today().sortedBy { it.sortOrder }
                _state.update { it.copy(isLoading = false, stops = stops, error = null) }
            } catch (e: NetworkException) {
                _state.update { it.copy(isLoading = false, error = e.error.messageEn) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load route") }
            }
        }
    }

    fun markTodoDone(customerId: String) {
        if (_state.value.markingId != null) return
        _state.update { it.copy(markingId = customerId) }
        viewModelScope.launch {
            try {
                val updated = api.markTodoDone(customerId)
                _state.update { s ->
                    s.copy(
                        markingId = null,
                        stops = s.stops.map { if (it.customerId == customerId) updated else it },
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(markingId = null) }
            }
        }
    }
}
