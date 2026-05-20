package com.jehadalomour.flowvan.shared.presentation.feature.voucherdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

data class VoucherDetailState(
    val entity: InvoiceEntity? = null,
    val lines: List<InvoiceLine> = emptyList(),
    val isLoading: Boolean = true,
)

class VoucherDetailViewModel(
    invoiceId: String,
    invoiceDao: InvoiceDao,
    private val json: Json,
) : ViewModel() {
    private val _state = MutableStateFlow(VoucherDetailState())
    val state: StateFlow<VoucherDetailState> = _state.asStateFlow()

    init {
        invoiceDao.observeById(invoiceId)
            .onEach { entity ->
                val lines = if (entity != null) {
                    runCatching { json.decodeFromString<List<InvoiceLine>>(entity.linesJson) }.getOrDefault(emptyList())
                } else emptyList()
                _state.update { it.copy(entity = entity, lines = lines, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
