package com.jehadalomour.flowvan.shared.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.GetDailyKpiUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getDailyKpi: GetDailyKpiUseCase,
    private val customers: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeRoute()
        load()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> load()
        }
    }

    private fun observeRoute() {
        customers.observeRoute()
            .onEach { route ->
                _state.update { it.copy(routeTopFive = route.take(5)) }
            }
            .launchIn(viewModelScope)
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = getCurrentUser()
            val kpi = getDailyKpi()
            _state.update { it.copy(user = user, kpi = kpi, isLoading = false) }
        }
    }
}
