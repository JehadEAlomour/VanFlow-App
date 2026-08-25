package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ErpFinanceRepository
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
import com.jehadalomour.flowvan.core.network.dto.ErpStatementDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountStatementViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
    private val erpFinance: ErpFinanceRepository,
    private val erpSync: ErpCustomerSync,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
        observeErpStatement()
        refreshErpForRange()
    }

    /**
     * The ERP's own statement (book of record) parsed from the offline cache and
     * shown as the authoritative view; the locally-computed ledger stays as the
     * offline fallback. Amounts are already JOD major units on the wire.
     */
    private fun observeErpStatement() {
        erpFinance.observeCustomer(customerId)
            .onEach { row ->
                val dto = row?.statementJson
                    ?.let { runCatching { json.decodeFromString(ErpStatementDto.serializer(), it) }.getOrNull() }
                _state.update {
                    if (dto != null && dto.isAvailable) {
                        it.copy(
                            erpAvailable = true,
                            erpAsOfMillis = row.asOfMillis,
                            erpOpeningBalance = dto.openingBalance ?: 0.0,
                            erpClosingBalance = dto.closingBalance ?: 0.0,
                            erpLines = dto.lines.map { l ->
                                ErpStatementUiLine(
                                    date = l.date, type = l.type, reference = l.reference,
                                    debit = l.debit, credit = l.credit, balance = l.balance,
                                )
                            },
                        )
                    } else {
                        it.copy(erpAvailable = false, erpAsOfMillis = row?.asOfMillis ?: 0L, erpLines = emptyList())
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /** Pull the ERP statement for the range on screen (best-effort; keeps cache offline). */
    private fun refreshErpForRange() {
        val s = _state.value
        viewModelScope.launch {
            erpSync.refresh(customerId, from = s.fromMillis.toYmd(), to = s.toMillis.toYmd())
        }
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
            is AccountStatementEvent.DateRangeChanged -> {
                _state.update { it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis, isLoading = true) }
                refreshErpForRange()
            }
        }
    }
}

/** Epoch-ms → "YYYY-MM-DD" in the device timezone, for the ERP statement window. */
@OptIn(ExperimentalTime::class)
private fun Long.toYmd(): String {
    val d = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val mm = if (d.monthNumber < 10) "0${d.monthNumber}" else d.monthNumber.toString()
    val dd = if (d.dayOfMonth < 10) "0${d.dayOfMonth}" else d.dayOfMonth.toString()
    return "${d.year}-$mm-$dd"
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
