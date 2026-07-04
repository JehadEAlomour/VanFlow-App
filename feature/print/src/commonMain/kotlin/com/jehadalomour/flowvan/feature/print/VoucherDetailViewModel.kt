package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.InvoiceLine
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
    val customer: Customer? = null,
    val isLoading: Boolean = true,
)

class VoucherDetailViewModel(
    invoiceId: String,
    invoiceDao: InvoiceDao,
    private val json: Json,
    private val customers: CustomerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VoucherDetailState())
    val state: StateFlow<VoucherDetailState> = _state.asStateFlow()

    init {
        invoiceDao.observeById(invoiceId)
            .onEach { entity ->
                val lines = if (entity != null) {
                    runCatching { json.decodeFromString<List<InvoiceLine>>(entity.linesJson) }.getOrDefault(emptyList())
                } else emptyList()
                val customer = entity?.let { customers.findById(it.customerId) }
                _state.update { it.copy(entity = entity, lines = lines, customer = customer, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
