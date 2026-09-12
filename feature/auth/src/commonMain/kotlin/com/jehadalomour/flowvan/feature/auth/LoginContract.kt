package com.jehadalomour.flowvan.feature.auth

import com.jehadalomour.flowvan.core.common.error.CashFlowError
import com.jehadalomour.flowvan.core.model.User

data class LoginState(
    val phone: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: CashFlowError? = null,
)

sealed interface LoginEvent {
    data class PhoneChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object Submit : LoginEvent
    data object DismissError : LoginEvent

    /**
     * Take the rep to the settings page that can fix their location.
     *
     * WHICH page is decided from the error being shown, not chosen by the
     * screen: a denied permission and a switched-off location service are fixed
     * in two different places, and sending someone to the wrong one lands them
     * somewhere that already looks correct.
     */
    data object OpenLocationSettings : LoginEvent
}

sealed interface LoginEffect {
    data class NavigateHome(val user: User) : LoginEffect
}
