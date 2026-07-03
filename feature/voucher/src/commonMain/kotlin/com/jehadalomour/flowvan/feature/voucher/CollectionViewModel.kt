package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.domain.usecase.CollectionValidationException
import com.jehadalomour.flowvan.core.domain.usecase.RecordCollectionUseCase
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.getString
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

    // Pre-fill the amount with the customer's outstanding balance once, on first load.
    private var didPrefillAmount = false

    init {
        customers.observeById(customerId)
            .onEach { c ->
                val prefill = !didPrefillAmount && c != null && _state.value.amountText.isBlank()
                if (c != null) didPrefillAmount = true
                _state.update {
                    it.copy(
                        customer = c,
                        amountText = if (prefill && c!!.balance > 0.0) amountToText(c.balance) else it.amountText,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Render a balance as a clean editable amount string (whole numbers without ".0", else up to fils). */
    private fun amountToText(a: Double): String =
        if (a == a.toLong().toDouble()) a.toLong().toString()
        else (kotlin.math.round(a * 1000) / 1000.0).toString()

    fun onEvent(event: CollectionEvent) {
        when (event) {
            is CollectionEvent.AmountChanged -> _state.update {
                it.copy(amountText = event.text.filter { c -> c.isDigit() || c == '.' })
            }
            is CollectionEvent.MethodSelected -> _state.update { it.copy(method = event.method) }

            is CollectionEvent.ChequeAmountChanged -> updateCheque(event.index) {
                copy(amountText = event.text.filter { c -> c.isDigit() || c == '.' })
            }
            is CollectionEvent.ChequeNumberChanged -> updateCheque(event.index) { copy(number = event.v) }
            is CollectionEvent.ChequeDateChanged -> updateCheque(event.index) { copy(dateMillis = event.epochMillis) }
            is CollectionEvent.OpenBankSheet -> _state.update {
                it.copy(bankSheetOpenForIndex = event.index, bankSearchQuery = "")
            }
            is CollectionEvent.CloseBankSheet -> _state.update {
                it.copy(bankSheetOpenForIndex = null, bankSearchQuery = "")
            }
            is CollectionEvent.BankSearchQueryChanged -> _state.update { it.copy(bankSearchQuery = event.query) }
            is CollectionEvent.BankSelected -> {
                val idx = _state.value.bankSheetOpenForIndex ?: return
                updateCheque(idx) { copy(bank = event.bank) }
                _state.update { it.copy(bankSheetOpenForIndex = null, bankSearchQuery = "") }
            }
            is CollectionEvent.AddCheque -> _state.update { it.copy(cheques = it.cheques + ChequeEntry()) }
            is CollectionEvent.RemoveCheque -> _state.update { s ->
                if (s.cheques.size <= 1) s
                else s.copy(cheques = s.cheques.toMutableList().also { it.removeAt(event.index) })
            }

            is CollectionEvent.TransferRefChanged -> _state.update { it.copy(transferRef = event.v) }
            is CollectionEvent.QuickFillAmount -> _state.update {
                val a = event.amount
                it.copy(amountText = if (a == a.toLong().toDouble()) a.toLong().toString() else a.toString())
            }
            is CollectionEvent.NotesChanged -> _state.update { it.copy(notes = event.v) }
            CollectionEvent.Save -> save()
            CollectionEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun updateCheque(index: Int, transform: ChequeEntry.() -> ChequeEntry) {
        _state.update { s ->
            if (index !in s.cheques.indices) return@update s
            s.copy(cheques = s.cheques.toMutableList().also { it[index] = s.cheques[index].transform() })
        }
    }

    private fun save() {
        _state.update { it.copy(isSaving = true) }
        val s = _state.value
        viewModelScope.launch {
            if (s.method == PaymentMethod.CHEQUE) saveCheques(s) else saveSingle(s)
        }
    }

    private suspend fun saveSingle(s: CollectionState) {
        val amount = s.amountText.toDoubleOrNull()
        if (amount == null) {
            val msg = getString(Res.string.err_enter_valid_amount)
            _state.update { it.copy(isSaving = false, errorAr = msg) }; return
        }
        val result = recordCollection(
            customerId = customerId,
            salesmanId = session.currentUserId.orEmpty(),
            amount = amount,
            method = s.method,
            chequeNumber = null,
            chequeBank = null,
            chequeDate = null,
            transferRef = s.transferRef.takeIf { it.isNotBlank() },
            notes = s.notes.takeIf { it.isNotBlank() },
        )
        result.fold(
            onSuccess = { entity -> _state.update { it.copy(isSaving = false, savedNumber = entity.number) } },
            onFailure = { ex ->
                val msg = (ex as? CollectionValidationException)?.messageAr ?: getString(Res.string.err_unexpected)
                _state.update { it.copy(isSaving = false, errorAr = msg) }
            },
        )
    }

    private suspend fun saveCheques(s: CollectionState) {
        val cheques = s.cheques
        cheques.forEachIndexed { idx, c ->
            if (c.amount == null || c.amount!! <= 0) {
                val msg = getString(Res.string.err_enter_valid_cheque_amount, idx + 1)
                _state.update { it.copy(isSaving = false, errorAr = msg) }; return
            }
            if (c.number.isBlank()) {
                val msg = getString(Res.string.err_enter_cheque_number, idx + 1)
                _state.update { it.copy(isSaving = false, errorAr = msg) }; return
            }
            if (c.bank == null) {
                val msg = getString(Res.string.err_select_cheque_bank, idx + 1)
                _state.update { it.copy(isSaving = false, errorAr = msg) }; return
            }
        }
        var lastNumber: String? = null
        for (c in cheques) {
            val result = recordCollection(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                amount = c.amount!!,
                method = PaymentMethod.CHEQUE,
                chequeNumber = c.number,
                chequeBank = c.bank!!.nameAr,
                chequeDate = c.dateMillis,
                transferRef = null,
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { lastNumber = it.number },
                onFailure = { ex ->
                    val msg = (ex as? CollectionValidationException)?.messageAr ?: getString(Res.string.err_unexpected)
                    _state.update { it.copy(isSaving = false, errorAr = msg) }; return
                },
            )
        }
        _state.update { it.copy(isSaving = false, savedNumber = lastNumber) }
    }
}
