package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CustomerDashboardViewModel(
    private val customerId: String,
    customers: CustomerRepository,
    invoices: InvoiceRepository,
    payments: PaymentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerDashboardState())
    val state: StateFlow<CustomerDashboardState> = _state.asStateFlow()

    init {
        customers.observeById(customerId)
            .onEach { c -> _state.update { it.copy(customer = c, isLoading = c == null) } }
            .launchIn(viewModelScope)

        invoices.observeByCustomerAndType(customerId, "SALE")
            .onEach { list -> _state.update { it.copy(sales = list) } }
            .launchIn(viewModelScope)

        invoices.observeByCustomerAndType(customerId, "RETURN")
            .onEach { list -> _state.update { it.copy(returns = list) } }
            .launchIn(viewModelScope)

        invoices.observeByCustomerAndType(customerId, "REQUEST")
            .onEach { list -> _state.update { it.copy(requests = list) } }
            .launchIn(viewModelScope)

        payments.observeByCustomer(customerId)
            .onEach { list -> _state.update { it.copy(payments = list) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: CustomerDashboardEvent) {
        when (event) {
            is CustomerDashboardEvent.TabSelected -> _state.update { it.copy(selectedTab = event.tab) }
        }
    }
}
