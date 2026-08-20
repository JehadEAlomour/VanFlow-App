package com.jehadalomour.flowvan.feature.prospecting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.haversineMeters
import com.jehadalomour.flowvan.core.network.api.ProspectingApi
import com.jehadalomour.flowvan.core.network.dto.CreateProspectSearchDto
import com.jehadalomour.flowvan.core.network.dto.ProspectDto
import com.jehadalomour.flowvan.core.network.http.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Drives the find-customers screen: read GPS, load the category chips, run a
 * 2 km search, and hand back the shops nearest first. All the heavy work —
 * Places, phone lookup, de-dup — is the server's; this only asks and orders.
 */
class FindCustomersViewModel(
    private val api: ProspectingApi,
    private val location: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(FindCustomersState())
    val state: StateFlow<FindCustomersState> = _state.asStateFlow()

    init {
        loadCategories()
        locate()
    }

    fun onEvent(e: FindCustomersEvent) {
        when (e) {
            FindCustomersEvent.RequestLocation -> locate()
            is FindCustomersEvent.ToggleCategory -> _state.update {
                val next = it.selectedCategories.toMutableSet()
                if (!next.add(e.key)) next.remove(e.key)
                it.copy(selectedCategories = next)
            }
            is FindCustomersEvent.AddKeyword -> _state.update {
                val term = e.term.trim()
                if (term.isEmpty() || term in it.keywords) it
                else it.copy(keywords = it.keywords + term)
            }
            is FindCustomersEvent.RemoveKeyword -> _state.update {
                it.copy(keywords = it.keywords - e.term)
            }
            FindCustomersEvent.Search -> search()
            FindCustomersEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = api.categories()
                _state.update { it.copy(featuredCategories = cats.featured) }
            } catch (_: Exception) {
                // The chips are a convenience — a keyword still searches — so a
                // failure here must not block the screen.
            }
        }
    }

    private fun locate() {
        _state.update { it.copy(isLocating = true, locationError = false) }
        viewModelScope.launch {
            val fix = location.lastLocation()
            _state.update {
                if (fix == null) it.copy(isLocating = false, locationError = true)
                else it.copy(isLocating = false, lat = fix.lat, lng = fix.lng, locationError = false)
            }
        }
    }

    private fun search() {
        val s = _state.value
        if (!s.canSearch) return
        val here = LatLng(s.lat!!, s.lng!!)
        _state.update { it.copy(isSearching = true, errorAr = null) }
        viewModelScope.launch {
            try {
                val res = api.search(
                    CreateProspectSearchDto(
                        lat = s.lat,
                        lng = s.lng,
                        radiusM = s.radiusM,
                        categories = s.selectedCategories.takeIf { it.isNotEmpty() }?.toList(),
                        keywords = s.keywords.takeIf { it.isNotEmpty() },
                    ),
                )
                _state.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        results = res.prospects.map { p -> p.withDistance(here) }
                            .sortedBy { n -> n.distanceM ?: Int.MAX_VALUE },
                    )
                }
            } catch (e: NetworkException) {
                _state.update { it.copy(isSearching = false, errorAr = e.error.messageAr) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, errorAr = e.message) }
            }
        }
    }

    // Distance is a client concern — the API does not return it, and the rep
    // wants "how far from me", which only the phone knows.
    private fun ProspectDto.withDistance(from: LatLng): NearbyProspect {
        val d = latitude?.let { la ->
            longitude?.let { lo -> haversineMeters(from, LatLng(la, lo)).roundToInt() }
        }
        return NearbyProspect(this, d)
    }
}
