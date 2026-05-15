package com.jehadalomour.flowvan.shared.presentation.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.shared.data.location.LocationProvider
import com.jehadalomour.flowvan.shared.domain.error.CashFlowError
import com.jehadalomour.flowvan.shared.domain.usecase.AuthException
import com.jehadalomour.flowvan.shared.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val log = Logger.withTag("LoginViewModel")

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> _state.update {
                it.copy(phone = event.value.filter { c -> c.isDigit() }.take(10), error = null)
            }
            is LoginEvent.PasswordChanged -> _state.update {
                it.copy(password = event.value, error = null)
            }
            LoginEvent.TogglePasswordVisibility -> _state.update {
                it.copy(passwordVisible = !it.passwordVisible)
            }
            LoginEvent.DismissError -> _state.update { it.copy(error = null) }
            LoginEvent.Submit -> submit()
        }
    }

    private fun submit() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val location = runCatching { locationProvider.lastLocation() }
                .onFailure { log.w(it) { "location lookup failed" } }
                .getOrNull()
            val result = loginUseCase(
                phone = _state.value.phone,
                password = _state.value.password,
                lat = location?.lat,
                lng = location?.lng,
            )
            result.fold(
                onSuccess = { user ->
                    _state.update { it.copy(isSubmitting = false, error = null, password = "") }
                    _effects.tryEmit(LoginEffect.NavigateHome(user))
                },
                onFailure = { throwable ->
                    val err = (throwable as? AuthException)?.error ?: CashFlowError.Unknown
                    _state.update { it.copy(isSubmitting = false, error = err) }
                },
            )
        }
    }
}
