package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.dto.CreateCustomerRequest
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create a new customer for the field: name + phone + a picked GPS location.
 * Posts to the backend (which assigns the customer number), caches the returned
 * customer locally so the list refreshes reactively, and exposes [savedCustomerId]
 * so the UI can navigate straight to the new customer's page. The customer is
 * assigned to the logged-in salesman via `repId`.
 */
class CreateCustomerViewModel(
    private val customerApi: CustomerApi,
    private val customers: CustomerRepository,
    private val session: SessionStore,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCustomerState())
    val state: StateFlow<CreateCustomerState> = _state.asStateFlow()

    fun onEvent(event: CreateCustomerEvent) {
        when (event) {
            is CreateCustomerEvent.NameChanged -> _state.update { it.copy(name = event.v) }
            is CreateCustomerEvent.PhoneChanged ->
                _state.update { it.copy(phone = event.v.filter { c -> c.isDigit() || c == '+' }) }
            CreateCustomerEvent.CaptureLocation -> captureLocation()
            CreateCustomerEvent.ClearLocation -> _state.update { it.copy(lat = null, lng = null) }
            CreateCustomerEvent.Save -> save()
            CreateCustomerEvent.DismissError -> _state.update { it.copy(errorAr = null, locationErrorAr = null) }
        }
    }

    private fun captureLocation() {
        _state.update { it.copy(isCapturingLocation = true, locationErrorAr = null) }
        viewModelScope.launch {
            val loc = locationProvider.lastLocation()
            _state.update {
                if (loc == null) it.copy(isCapturingLocation = false, locationErrorAr = LOCATION_UNAVAILABLE)
                else it.copy(isCapturingLocation = false, lat = loc.lat, lng = loc.lng)
            }
        }
    }

    private fun save() {
        val s = _state.value
        val name = s.name.trim()
        if (name.length < 2) {
            _state.update { it.copy(errorAr = ERR_NAME) }
            return
        }
        _state.update { it.copy(isSaving = true, errorAr = null) }
        viewModelScope.launch {
            val result = runCatching {
                val dto = customerApi.create(
                    CreateCustomerRequest(
                        customerName = name,
                        nameAr = name,
                        phone = s.phone.trim().takeIf { it.isNotBlank() },
                        latitude = s.lat?.toString(),
                        longitude = s.lng?.toString(),
                        repId = session.currentRepId?.takeIf { it.isNotBlank() },
                    ),
                )
                // Cache the server-returned customer so the list updates reactively.
                customers.save(dto.toEntity())
                dto.id
            }
            result.fold(
                onSuccess = { id -> _state.update { it.copy(isSaving = false, savedCustomerId = id) } },
                onFailure = { _state.update { it.copy(isSaving = false, errorAr = ERR_SAVE) } },
            )
        }
    }

    private companion object {
        const val ERR_NAME = "أدخل اسم العميل (حرفان على الأقل)"
        const val ERR_SAVE = "تعذّر حفظ العميل. تحقق من الاتصال وحاول مرة أخرى."
        const val LOCATION_UNAVAILABLE = "تعذّر تحديد الموقع. تأكد من تفعيل GPS والصلاحيات."
    }
}
