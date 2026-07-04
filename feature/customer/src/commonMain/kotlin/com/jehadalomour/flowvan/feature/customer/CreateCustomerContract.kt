package com.jehadalomour.flowvan.feature.customer

data class CreateCustomerState(
    val name: String = "",
    val phone: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationErrorAr: String? = null,
    val isSaving: Boolean = false,
    val savedCustomerId: String? = null,
    val errorAr: String? = null,
) {
    val canSave: Boolean get() = name.trim().length >= 2 && !isSaving
    val hasLocation: Boolean get() = lat != null && lng != null
}

sealed interface CreateCustomerEvent {
    data class NameChanged(val v: String) : CreateCustomerEvent
    data class PhoneChanged(val v: String) : CreateCustomerEvent
    data object CaptureLocation : CreateCustomerEvent
    data object ClearLocation : CreateCustomerEvent
    data object Save : CreateCustomerEvent
    data object DismissError : CreateCustomerEvent
}
