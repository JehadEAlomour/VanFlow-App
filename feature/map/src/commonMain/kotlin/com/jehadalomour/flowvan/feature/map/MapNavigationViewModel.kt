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

/**
 * Guides to a customer OR to a raw point.
 *
 * A customer is loaded by id; a prospect from the search arrives as coordinates
 * with no id, because it is not a customer yet. Exactly one of [customerId] or
 * [point] is set — the factory in [mapModule] picks the constructor from the
 * navigation arguments.
 */
class MapNavigationViewModel private constructor(
    private val customerId: String?,
    private val point: Point?,
    private val customers: CustomerRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    data class Point(val lat: Double, val lng: Double, val label: String)

    /** Navigate to a saved customer. */
    constructor(
        customerId: String,
        customers: CustomerRepository,
        locationProvider: LocationProvider,
    ) : this(customerId, null, customers, locationProvider)

    /** Navigate to a bare point (a prospect with no customer row). */
    constructor(
        point: Point,
        customers: CustomerRepository,
        locationProvider: LocationProvider,
    ) : this(null, point, customers, locationProvider)

    private val _state = MutableStateFlow(MapNavigationState())
    val state: StateFlow<MapNavigationState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            if (customerId != null) {
                val customer = customers.findById(customerId)
                _state.update {
                    it.copy(
                        customer = customer,
                        hasCoordinates = customer?.lat != null && customer.lng != null,
                    )
                }
            } else if (point != null) {
                _state.update {
                    it.copy(
                        pointLat = point.lat,
                        pointLng = point.lng,
                        pointLabel = point.label,
                        hasCoordinates = true,
                    )
                }
            }
            val location = locationProvider.lastLocation()
            _state.update { it.copy(userLocation = location, isLoadingLocation = false) }
        }
    }
}
