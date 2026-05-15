package com.jehadalomour.flowvan.shared.presentation.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.usecase.CollectionValidationException
import com.jehadalomour.flowvan.shared.domain.usecase.RecordCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val customerId: String,
    customers: CustomerRepository,
    private val session: SessionStore,
    private val recordCollection: RecordCollectionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionState())
    val state: StateFlow<CollectionState> = _state.asStateFlow()

    init {
        customers.observeById(customerId)
            .onEach { c -> _state.update { it.copy(customer = c) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: CollectionEvent) {
        when (event) {
            is CollectionEvent.AmountChanged -> _state.update {
                it.copy(amountText = event.text.filter { c -> c.isDigit() || c == '.' })
            }
            is CollectionEvent.MethodSelected -> _state.update { it.copy(method = event.method) }
            is CollectionEvent.ChequeNumberChanged -> _state.update { it.copy(chequeNumber = event.v) }
            is CollectionEvent.ChequeBankChanged -> _state.update { it.copy(chequeBank = event.v) }
            is CollectionEvent.ChequeDateChanged -> _state.update { it.copy(chequeDate = event.epochMillis) }
            is CollectionEvent.TransferRefChanged -> _state.update { it.copy(transferRef = event.v) }
            is CollectionEvent.NotesChanged -> _state.update { it.copy(notes = event.v) }
            CollectionEvent.Save -> save()
            CollectionEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun save() {
        val s = _state.value
        val amount = s.amount
        if (amount == null) {
            _state.update { it.copy(errorAr = "أدخل مبلغ صحيح") }; return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = recordCollection(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                amount = amount,
                method = s.method,
                chequeNumber = s.chequeNumber.takeIf { it.isNotBlank() },
                chequeBank = s.chequeBank.takeIf { it.isNotBlank() },
                chequeDate = s.chequeDate,
                transferRef = s.transferRef.takeIf { it.isNotBlank() },
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { entity ->
                    _state.update { it.copy(isSaving = false, savedNumber = entity.number) }
                },
                onFailure = { ex ->
                    val msg = (ex as? CollectionValidationException)?.messageAr ?: "حدث خطأ غير متوقع"
                    _state.update { it.copy(isSaving = false, errorAr = msg) }
                },
            )
        }
    }
}
