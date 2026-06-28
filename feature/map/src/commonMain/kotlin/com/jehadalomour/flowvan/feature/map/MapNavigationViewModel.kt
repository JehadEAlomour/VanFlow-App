package com.jehadalomour.flowvan.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapNavigationViewModel(
    private val customerId: String,
    private val customers: CustomerRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(MapNavigationState())
    val state: StateFlow<MapNavigationState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val customer = customers.findById(customerId)
            _state.update { it.copy(
                customer = customer,
                hasCoordinates = customer?.lat != null && customer.lng != null,
            ) }

            val location = locationProvider.lastLocation()
            _state.update { it.copy(userLocation = location, isLoadingLocation = false) }
        }
    }
}
