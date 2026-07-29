package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountStatementViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountStatementState(
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        )
    )
    val state: StateFlow<AccountStatementState> = _state.asStateFlow()

    init {
        observeCustomer()
        observeEntries()
    }

    private fun observeCustomer() {
        customerRepository.observeById(customerId)
            .onEach { customer -> _state.update { it.copy(customer = customer) } }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEntries() {
        _state
            .map { it.fromMillis to it.toMillis }
            .distinctUntilChanged()
            .flatMapLatest { (from, to) ->
                combine(
                    invoiceDao.observeByCustomerRange(customerId, from, to),
                    paymentDao.observeByCustomerRange(customerId, from, to),
                ) { invoices, payments ->
                    val entries = buildList {
                        invoices.forEach { inv ->
                            // Only ON-ACCOUNT (credit) sales/returns belong on a receivable
                            // statement. Cash/cheque/transfer sales are settled at point of sale —
                            // they create no receivable (only CREDIT adjusts the balance, see
                            // CreateSaleVoucherUseCase), so they must never appear here or move the
                            // total. paymentMethod null → keep (unknown/legacy, don't hide silently).
                            val pm = inv.paymentMethod
                            val settledAtPos = (inv.type == "SALE" || inv.type == "RETURN") &&
                                pm != null && pm != "CREDIT"
                            if (!settledAtPos) add(StatementEntry.Invoice(inv))
                        }
                        payments.forEach { add(StatementEntry.Payment(it)) }
                    }.sortedByDescending { it.createdAt }
                    entries
                }
            }
            .onEach { entries -> _state.update { it.copy(entries = entries, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AccountStatementEvent) {
        when (event) {
            is AccountStatementEvent.DateRangeChanged ->
                _state.update { it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis, isLoading = true) }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun startOfMonthMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
    return LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
private fun endOfTodayMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
    return today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds() - 1
}
