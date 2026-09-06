package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerStatementRepository
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
import com.jehadalomour.flowvan.core.model.ledger.StatementDocType
import com.jehadalomour.flowvan.core.model.ledger.StatementMovement
import com.jehadalomour.flowvan.core.model.ledger.StatementSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * كشف الحساب for one customer.
 *
 * **The server first, this device second.** The account belongs to the office, not to
 * the handset: Room holds only the vouchers and receipts this phone created, so a shop
 * served by a second van — or a rep who reinstalled the app — saw a statement missing
 * most of its own history and a closing balance to match. So the statement is fetched
 * whenever there is a connection, and the local ledger is the fallback that keeps the
 * screen usable in a basement, labelled as such.
 *
 * The local path stays a live Room stream rather than a one-shot read: a sale made
 * moments ago has to appear on the statement without the rep leaving the screen and
 * coming back.
 */
class AccountStatementViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val statements: CustomerStatementRepository,
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

    /** The customer's CODE — what vouchers are keyed by. Blank until they load. */
    private var customerNumber: String = ""

    /** The server's answer for the range on screen; null until (or unless) it lands. */
    private var live: StatementSnapshot? = null

    /**
     * A server fetch is expected or in flight, so Room must not publish underneath it.
     *
     * Without this the local ledger lands first every time — Room answers in
     * milliseconds — and the rep watches the "incomplete statement" warning appear and
     * then vanish a second later on a perfectly good connection. Starts true because
     * the intent to fetch exists before the customer's code has even loaded.
     */
    private var awaitingServer = true

    /** The same range rebuilt from Room, kept current by the stream below. */
    private var local: StatementSnapshot = StatementSnapshot()

    private var loadJob: Job? = null

    init {
        observeCustomer()
        observeLocal()
    }

    private fun observeCustomer() {
        customerRepository.observeById(customerId)
            .onEach { customer ->
                _state.update { it.copy(customer = customer) }
                val code = customer?.code.orEmpty()
                // The code arrives after the screen does, and it is what the voucher
                // endpoint identifies a customer by — so the first fetch waits for it
                // rather than asking the server about a blank customer.
                if (code.isNotBlank() && code != customerNumber) {
                    customerNumber = code
                    fetch()
                } else if (customer != null && code.isBlank()) {
                    // A customer with no code cannot be looked up on the server at
                    // all, so stop waiting for an answer that will never come.
                    awaitingServer = false
                    publish(local)
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLocal() {
        _state
            .map { it.fromMillis to it.toMillis }
            .distinctUntilChanged()
            .flatMapLatest { (from, to) ->
                // Four streams: the period's movement, and everything before it —
                // the latter collapsed to a single opening balance rather than
                // listed.
                combine(
                    invoiceDao.observeByCustomerRange(customerId, from, to),
                    paymentDao.observeByCustomerRange(customerId, from, to),
                    invoiceDao.observeByCustomerRange(customerId, 0L, from - 1),
                    paymentDao.observeByCustomerRange(customerId, 0L, from - 1),
                ) { invoices, payments, priorInvoices, priorPayments ->
                    StatementSnapshot(
                        openingBalance = movementsOf(priorInvoices, priorPayments)
                            .sumOf { it.movement },
                        movements = movementsOf(invoices, payments).sortedBy { it.createdAt },
                        isLive = false,
                    )
                }
            }
            .onEach { snapshot ->
                local = snapshot
                // The server's answer wins while it stands, and nothing shows under a
                // fetch that is still running. Local emissions are still recorded
                // above, so a retry that fails falls back to something current.
                if (live == null && !awaitingServer) publish(snapshot)
            }
            .launchIn(viewModelScope)
    }

    /** Ask the server for the range on screen; fall back to Room when it cannot answer. */
    private fun fetch() {
        val s = _state.value
        if (customerNumber.isBlank()) return
        loadJob?.cancel()
        live = null
        awaitingServer = true
        _state.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            val snapshot = statements.load(
                customerNumber = customerNumber,
                customerId = customerId,
                fromMillis = s.fromMillis,
                toMillis = s.toMillis,
            )
            live = snapshot
            awaitingServer = false
            publish(snapshot ?: local)
        }
    }

    private fun publish(snapshot: StatementSnapshot) {
        _state.update {
            it.copy(
                openingBalance = snapshot.openingBalance,
                lines = linesOf(snapshot),
                isLoading = false,
                source = if (snapshot.isLive) StatementSource.LIVE else StatementSource.LOCAL,
            )
        }
    }

    // What belongs on the ledger, and what it does to the balance, is [CustomerStatement]
    // — one rule shared with the printed statement and matching the office dashboard.
    // Orders are not on it: an order is a promise of goods, not a debt.

    private fun movementsOf(
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
    ): List<StatementMovement> = buildList {
        invoices.filter(CustomerStatement::isLedgerEntry).forEach { inv ->
            // Anything that is neither a sale nor a return moves no receivable, and a
            // row that moves nothing cannot be labelled honestly — see the default in
            // CustomerStatement.movement.
            val docType = when (inv.type) {
                CustomerStatement.TYPE_SALE -> StatementDocType.SALE
                CustomerStatement.TYPE_RETURN -> StatementDocType.RETURN
                else -> return@forEach
            }
            add(
                StatementMovement(
                    id = inv.id,
                    number = inv.number,
                    createdAt = inv.createdAt,
                    docType = docType,
                    debit = if (docType == StatementDocType.SALE) inv.total else 0.0,
                    credit = if (docType == StatementDocType.RETURN) inv.total else 0.0,
                    isLocal = true,
                ),
            )
        }
        payments.forEach { pay ->
            add(
                StatementMovement(
                    id = pay.id,
                    number = pay.number,
                    createdAt = pay.createdAt,
                    docType = StatementDocType.PAYMENT,
                    credit = pay.amount,
                    method = pay.method,
                    chequeDate = pay.chequeDate,
                    isLocal = true,
                ),
            )
        }
    }

    /**
     * Walked oldest-first so each line can carry the balance as it stood after it,
     * then reversed: the list reads newest first, but a running balance only has a
     * meaning when accumulated forwards.
     */
    private fun linesOf(snapshot: StatementSnapshot): List<StatementLine> {
        var running = snapshot.openingBalance
        return snapshot.movements.map { entry ->
            running += entry.movement
            StatementLine(entry = entry, balanceAfter = running)
        }.asReversed()
    }

    fun onEvent(event: AccountStatementEvent) {
        when (event) {
            is AccountStatementEvent.DateRangeChanged -> {
                _state.update {
                    it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis, isLoading = true)
                }
                fetch()
            }

            AccountStatementEvent.Retry -> fetch()
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
