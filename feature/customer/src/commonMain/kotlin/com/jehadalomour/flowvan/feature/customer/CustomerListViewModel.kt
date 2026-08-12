package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CustomerListViewModel(
    private val customers: CustomerRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListState(canAddCustomer = session.canAddCustomer))
    val state: StateFlow<CustomerListState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observe()
        observeQuery()
    }

    fun onEvent(event: CustomerListEvent) {
        when (event) {
            is CustomerListEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                queryFlow.value = event.query
            }
            is CustomerListEvent.FilterChanged -> {
                _state.update { it.copy(filter = event.filter) }
                recompute()
            }
            CustomerListEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
                queryFlow.value = ""
                recompute()
            }
        }
    }

    private fun observe() {
        customers.observeAll()
            .onEach { list ->
                _state.update { it.copy(all = list, isLoading = false) }
                recompute()
            }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        queryFlow
            .debounce(300)
            .onEach { recompute() }
            .launchIn(viewModelScope)
    }

    private fun recompute() {
        val s = _state.value
        val q = s.searchQuery.trim().lowercase()
        val visible = s.all.filter { c ->
            val matchesQuery = q.isEmpty() ||
                c.nameAr.lowercase().contains(q) ||
                (c.nameEn?.lowercase()?.contains(q) == true) ||
                c.code.lowercase().contains(q) ||
                c.area.lowercase().contains(q)
            val matchesFilter = when (s.filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.ON_ROUTE -> c.isOnRoute
                CustomerFilter.OWING -> c.balance > 0
                // A limit of zero means "no limit set", not "everything is over
                // it" — without that guard every customer with any balance would
                // show as blocked from credit.
                CustomerFilter.OVER_LIMIT -> c.creditLimit > 0 && c.balance >= c.creditLimit
            }
            matchesQuery && matchesFilter
        }
        _state.update { it.copy(visible = visible) }
    }

}
