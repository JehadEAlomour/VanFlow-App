package com.jehadalomour.flowvan.shared.presentation.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.UserRepository
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class VoucherPrintViewModel(
    private val invoiceId: String,
    private val invoiceDao: InvoiceDao,
    private val customers: CustomerRepository,
    private val users: UserRepository,
    private val appSettings: AppSettingsRepository,
    private val json: Json,
) : ViewModel() {

    private val _state = MutableStateFlow(VoucherPrintState())
    val state: StateFlow<VoucherPrintState> = _state.asStateFlow()

    init {
        invoiceDao.observeById(invoiceId)
            .onEach { entity ->
                if (entity == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@onEach
                }
                val lines = runCatching {
                    json.decodeFromString<List<InvoiceLine>>(entity.linesJson)
                }.getOrDefault(emptyList())

                val customer = customers.findById(entity.customerId)
                val salesman = users.findById(entity.salesmanId)
                val settings = appSettings.get()

                _state.update {
                    VoucherPrintState(
                        isLoading = false,
                        invoiceId = entity.id,
                        number = entity.number,
                        type = entity.type,
                        paymentMethod = entity.paymentMethod,
                        createdAt = entity.createdAt,
                        customerNameAr = customer?.nameAr.orEmpty(),
                        customerCode = customer?.code.orEmpty(),
                        customerTaxNumber = customer?.taxNumber,
                        salesmanNameAr = salesman?.nameAr.orEmpty(),
                        lines = lines,
                        subtotal = entity.subtotal,
                        discountAmount = entity.discountAmount,
                        taxAmount = entity.taxAmount,
                        total = entity.total,
                        notes = entity.notes,
                        branch = settings.branch,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
