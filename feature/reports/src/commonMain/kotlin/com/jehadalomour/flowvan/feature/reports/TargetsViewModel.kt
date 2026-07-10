package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.TargetRepository
import com.jehadalomour.flowvan.core.model.SalesTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TargetsState(
    val isLoading: Boolean = true,
    val current: SalesTarget? = null,   // this month (history[0])
    val history: List<SalesTarget> = emptyList(),
    val error: String? = null,
)

/** The signed-in salesman's own target + read-only history (from /targets/me/history). */
class TargetsViewModel(
    private val repository: TargetRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TargetsState())
    val state: StateFlow<TargetsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.myHistory(months = 6) }
                .onSuccess { list ->
                    _state.update {
                        it.copy(isLoading = false, current = list.firstOrNull(), history = list)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "error") }
                }
        }
    }
}
