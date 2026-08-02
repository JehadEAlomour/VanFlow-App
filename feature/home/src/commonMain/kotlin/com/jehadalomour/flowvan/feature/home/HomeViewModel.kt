package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.LocationPointDao
import com.jehadalomour.flowvan.core.database.dao.ShiftDao
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import com.jehadalomour.flowvan.core.domain.tracking.LocationTrackingCoordinator
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RefreshCatalogUseCase
import com.jehadalomour.flowvan.core.domain.usecase.StartShiftUseCase
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
    private val refreshCatalog: RefreshCatalogUseCase,
    private val locationPointDao: LocationPointDao,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // Keep pushing pending offline transactions + retrying on reconnect, shift or not.
        syncScheduler.start()
        // Always-on tracking: the trail starts the moment a signed-in user lands here,
        // shift or no shift. An active shift only relabels the points (observeActiveShift).
        sessionStore.currentUserId?.let { userId ->
            coordinator.start(LocationTrackingCoordinator.ALWAYS_ON_SHIFT_ID, userId)
        }
        observeRoute()
        observeActiveShift()
        observeSyncStatus()
        load()
    }

    private fun observeSyncStatus() {
        locationPointDao.observeUnsyncedCount()
            .onEach { count -> _state.update { it.copy(pendingPings = count) } }
            .launchIn(viewModelScope)
        syncScheduler.lastSyncAt
            .onEach { ts -> _state.update { it.copy(lastSyncAt = ts) } }
            .launchIn(viewModelScope)
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
                    // Restart so points carry the shift id while a shift is active.
                    if (!coordinator.isTracking) coordinator.start(shift.id, shift.userId)
                } else {
                    // No active shift → keep the trail alive on the always-on id, then open a
                    // shift automatically. The rep never has to press "بدء اليوم": the shift is
                    // always on for a signed-in user. StartShiftUseCase is idempotent and purely
                    // local, and inserting re-fires this observer with the new shift, which takes
                    // the branch above — so this cannot loop.
                    coordinator.start(LocationTrackingCoordinator.ALWAYS_ON_SHIFT_ID, userId)
                    handleStartShift()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleStartShift() {
        viewModelScope.launch {
            val shiftId = startShift()
            if (shiftId.isNotEmpty()) {
                val userId = sessionStore.currentUserId ?: return@launch
                // Relabel the trail with the new shift id (stop → start re-registers).
                coordinator.stop()
                coordinator.start(shiftId, userId)
            }
        }
    }

    private fun load() {
        // Show local data immediately…
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = getCurrentUser()
            val kpi = getDailyKpi()
            _state.update { it.copy(user = user, kpi = kpi, isLoading = false) }
        }
        // …and pull fresh data from the backend in the background, then recompute KPIs.
        viewModelScope.launch {
            if (refreshCatalog().getOrNull()?.skipped == false) {
                _state.update { it.copy(kpi = getDailyKpi()) }
            }
        }
    }
}
