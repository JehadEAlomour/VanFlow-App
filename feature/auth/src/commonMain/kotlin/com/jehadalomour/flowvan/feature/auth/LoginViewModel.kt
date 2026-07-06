package com.jehadalomour.flowvan.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import com.jehadalomour.flowvan.core.domain.usecase.AuthException
import com.jehadalomour.flowvan.core.domain.usecase.BackendLoginUseCase
import com.jehadalomour.flowvan.core.domain.usecase.BackupDatabaseUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val backendLogin: BackendLoginUseCase,
    private val backupDatabase: BackupDatabaseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> _state.update {
                // Backend logs in by userNumber (alphanumeric, up to 32 chars).
                it.copy(phone = event.value.trim().take(32), error = null)
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
            val result = backendLogin(
                userNumber = _state.value.phone,
                password = _state.value.password,
            )
            result.fold(
                onSuccess = { user ->
                    // Snapshot the local db into Documents on every successful login (best-effort).
                    backupDatabase()
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
