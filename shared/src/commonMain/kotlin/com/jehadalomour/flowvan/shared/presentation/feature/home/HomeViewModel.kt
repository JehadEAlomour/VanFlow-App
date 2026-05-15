package com.jehadalomour.flowvan.shared.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.ShiftDao
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.sync.SyncScheduler
import com.jehadalomour.flowvan.shared.domain.tracking.LocationTrackingCoordinator
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.StartShiftUseCase
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
    private val shiftDao: ShiftDao,
    private val sessionStore: SessionStore,
    private val startShift: StartShiftUseCase,
    private val coordinator: LocationTrackingCoordinator,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeRoute()
        observeActiveShift()
        load()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> load()
            HomeEvent.StartShift -> handleStartShift()
        }
    }

    private fun observeRoute() {
        customers.observeRoute()
            .onEach { route -> _state.update { it.copy(routeTopFive = route.take(5)) } }
            .launchIn(viewModelScope)
    }

    private fun observeActiveShift() {
        val userId = sessionStore.currentUserId ?: return
        shiftDao.observeActive(userId)
            .onEach { shift ->
                _state.update { it.copy(activeShift = shift) }
                if (shift != null) {
                    coordinator.start(shift.id, shift.userId)
                    syncScheduler.start()
                } else {
                    coordinator.stop()
                    syncScheduler.stop()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleStartShift() {
        viewModelScope.launch {
            val shiftId = startShift()
            if (shiftId.isNotEmpty()) {
                val userId = sessionStore.currentUserId ?: return@launch
                coordinator.start(shiftId, userId)
            }
        }
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
