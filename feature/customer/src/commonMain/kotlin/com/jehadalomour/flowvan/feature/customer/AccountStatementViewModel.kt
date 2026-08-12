package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
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
                // Four streams: the period's movement, and everything before it —
                // the latter collapsed to a single opening balance rather than
                // listed. Same shape as StatementPrintViewModel, so the screen and
                // the paper cannot disagree about what is owed.
                combine(
                    invoiceDao.observeByCustomerRange(customerId, from, to),
                    paymentDao.observeByCustomerRange(customerId, from, to),
                    invoiceDao.observeByCustomerRange(customerId, 0L, from - 1),
                    paymentDao.observeByCustomerRange(customerId, 0L, from - 1),
                ) { invoices, payments, priorInvoices, priorPayments ->
                    val opening = balanceOf(priorInvoices, priorPayments)
                    opening to linesOf(invoices, payments, opening)
                }
            }
            .onEach { (opening, lines) ->
                _state.update { it.copy(openingBalance = opening, lines = lines, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    // What belongs on the ledger, and what it does to the balance, is [CustomerStatement]
    // — one rule shared with the printed statement and matching the office dashboard.
    // Orders are not on it: an order is a promise of goods, not a debt.

    private fun balanceOf(invoices: List<InvoiceEntity>, payments: List<PaymentEntity>): Double {
        val ledger = invoices.filter(CustomerStatement::isLedgerEntry)
        return ledger.sumOf(CustomerStatement::movement) - payments.sumOf { it.amount }
    }

    /**
     * Walked oldest-first so each line can carry the balance as it stood after it,
     * then reversed: the list reads newest first, but a running balance only has a
     * meaning when accumulated forwards.
     */
    private fun linesOf(
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
        opening: Double,
    ): List<StatementLine> {
        val entries = buildList {
            invoices.filter(CustomerStatement::isLedgerEntry).forEach { add(StatementEntry.Invoice(it)) }
            payments.forEach { add(StatementEntry.Payment(it)) }
        }.sortedBy { it.createdAt }

        var running = opening
        return entries.map { entry ->
            // The same function the totals and the opening balance use, so the last
            // line's balance always equals openingBalance + net. A shopkeeper who adds
            // the column and lands somewhere other than the closing figure stops
            // trusting the paper, and an unrecognised voucher type must not be able to
            // cause that.
            val movement = when (entry) {
                is StatementEntry.Payment -> -entry.amount
                is StatementEntry.Invoice -> CustomerStatement.movement(entry.entity)
            }
            running += movement
            StatementLine(entry = entry, balanceAfter = running)
        }.asReversed()
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
