package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.dto.LogVisitRequest
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerDashboardViewModel(
    private val customerId: String,
    private val customers: CustomerRepository,
    invoices: InvoiceRepository,
    payments: PaymentRepository,
    private val customerApi: CustomerApi,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CustomerDashboardState(requireVisitReason = session.can("customers.visitReason")),
    )
    val state: StateFlow<CustomerDashboardState> = _state.asStateFlow()
    private val log = Logger.withTag("CustomerVisit")

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

    /**
     * Pull the latest customer record from the server so the board reflects an updated
     * balance/totals after returning from a sale, return, collection or invoice print.
     * Invoices/payments are already DB-observed and update reactively.
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                val dto = customerApi.getById(customerId)
                customers.cacheAll(listOf(dto.toEntity()))
            } catch (e: Exception) {
                log.w("customer refresh failed: ${e.message}")
            }
        }
    }

    fun onEvent(event: CustomerDashboardEvent) {
        when (event) {
            is CustomerDashboardEvent.TabSelected ->
                _state.update { it.copy(selectedTab = event.tab) }

            is CustomerDashboardEvent.LeaveRequested -> {
                if (event.hadTransaction) {
                    // Did business → record the visit and leave straight away.
                    logVisit(hadSale = true, note = null)
                    _state.update { it.copy(navigateBack = true) }
                } else {
                    // No transaction → require a reason (if permitted) else a confirm.
                    _state.update {
                        it.copy(
                            leaveDialog =
                                if (it.requireVisitReason) LeaveDialog.REASON else LeaveDialog.CONFIRM,
                        )
                    }
                }
            }

            is CustomerDashboardEvent.ConfirmLeave -> {
                logVisit(hadSale = false, note = event.reason?.trim()?.ifBlank { null })
                _state.update { it.copy(leaveDialog = LeaveDialog.NONE, navigateBack = true) }
            }

            CustomerDashboardEvent.DismissLeave ->
                _state.update { it.copy(leaveDialog = LeaveDialog.NONE) }
        }
    }

    /** Record the visit on the server so it reflects on the dashboard weekly route. */
    private fun logVisit(hadSale: Boolean, note: String?) {
        val repId = session.currentRepId ?: return
        viewModelScope.launch {
            try {
                customerApi.logVisit(
                    customerId,
                    LogVisitRequest(repId = repId, hadSale = hadSale, visitNote = note),
                )
            } catch (e: Exception) {
                log.w("logVisit failed: ${e.message}")
            }
        }
    }
}
