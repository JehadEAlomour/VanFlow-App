package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class ReceiptDetailState(
    val entity: PaymentEntity? = null,
    val isLoading: Boolean = true,
)

class ReceiptDetailViewModel(
    paymentId: String,
    paymentDao: PaymentDao,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptDetailState())
    val state: StateFlow<ReceiptDetailState> = _state.asStateFlow()

    init {
        paymentDao.observeById(paymentId)
            .onEach { entity -> _state.update { it.copy(entity = entity, isLoading = false) } }
            .launchIn(viewModelScope)
    }
}
