package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class ReceivablesReportState(
    val customers: List<Customer> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val count: Int = 0,
)

class ReceivablesReportViewModel(
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceivablesReportState())
    val state: StateFlow<ReceivablesReportState> = _state.asStateFlow()

    init {
        customerRepository.observeAll()
            .onEach { list ->
                val withBalance = list.filter { it.balance > 0.0 }
                    .sortedByDescending { it.balance }
                _state.update {
                    it.copy(
                        customers = withBalance,
                        totalBalance = withBalance.sumOf { c -> c.balance },
                        totalOverdue = withBalance.sumOf { c -> c.overdueAmount },
                        count = withBalance.size,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
