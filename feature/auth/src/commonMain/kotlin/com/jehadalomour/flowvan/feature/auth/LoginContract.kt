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
}

sealed interface LoginEffect {
    data class NavigateHome(val user: User) : LoginEffect
}
