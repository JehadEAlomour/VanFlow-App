package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.model.Customer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RouteViewModel(
    private val customers: CustomerRepository,
    private val invoices: InvoiceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RouteState())
    val state: StateFlow<RouteState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeRoute()
        observeQuery()
        loadVisitedCount()
    }

    fun onEvent(event: RouteEvent) {
        when (event) {
            is RouteEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                queryFlow.value = event.query
            }
            RouteEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }
                queryFlow.value = ""
            }
        }
    }

    private fun observeRoute() {
        customers.observeRoute()
            .onEach { list ->
                _state.update {
                    it.copy(
                        routeCustomers = list,
                        plannedCount = list.size,
                        isLoading = false,
                    )
                }
                applyFilter(queryFlow.value, list)
            }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        queryFlow
            .debounce(300)
            .onEach { applyFilter(it, _state.value.routeCustomers) }
            .launchIn(viewModelScope)
    }

    private fun applyFilter(query: String, list: List<Customer>) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) emptyList() else list.filter {
            it.nameAr.lowercase().contains(q) ||
                (it.nameEn?.lowercase()?.contains(q) == true) ||
                it.code.lowercase().contains(q) ||
                it.area.lowercase().contains(q)
        }
        _state.update { it.copy(searchResults = filtered) }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadVisitedCount() {
        viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
            val startOfToday = today.atStartOfDayIn(tz).toEpochMilliseconds()
            val visited = invoices.distinctCustomersSince(startOfToday)
            _state.update { it.copy(visitedCount = visited) }
        }
    }
}
