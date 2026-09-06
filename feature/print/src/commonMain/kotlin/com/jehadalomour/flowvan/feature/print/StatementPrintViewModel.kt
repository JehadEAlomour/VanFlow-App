package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerStatementRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.ledger.CustomerStatement
import com.jehadalomour.flowvan.core.model.ledger.StatementDocType
import com.jehadalomour.flowvan.core.model.ledger.StatementMovement
import com.jehadalomour.flowvan.core.model.ledger.StatementSnapshot
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The printable customer account statement (كشف حساب) for one date range.
 *
 * Reads what the on-screen statement reads, in the same order of preference: the
 * SERVER when there is a connection, this device's own ledger when there is not.
 * The paper and the screen must never disagree about a closing figure, and they
 * would the moment one of them was allowed a source the other did not have.
 *
 * Both paths apply the same rule about what belongs on a receivable: only
 * ON-ACCOUNT movement. A cash sale is settled at the counter and creates no
 * receivable, so putting it here would inflate both the debit column and the
 * closing balance — the customer would be handed a demand for money they already
 * paid.
 */
@OptIn(ExperimentalTime::class)
class StatementPrintViewModel(
    private val customerId: String,
    private val fromMillis: Long,
    private val toMillis: Long,
    private val customers: CustomerRepository,
    private val statements: CustomerStatementRepository,
    private val users: UserRepository,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        StatementPrintState(
            customerId = customerId,
            fromMillis = fromMillis,
            toMillis = toMillis,
            printedAt = Clock.System.now().toEpochMilliseconds(),
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<StatementPrintState> = _state.asStateFlow()

    /** The server's answer, once it lands. While it stands, Room does not overwrite it. */
    private var live: StatementSnapshot? = null

    /**
     * The server has not answered yet, so Room must not publish underneath it. Room
     * answers first every time, and a rep who tapped print in that window would send
     * the incomplete paper to the printer.
     */
    private var awaitingServer = true

    /** The same range rebuilt from Room, kept current by the stream in `init`. */
    private var local: StatementSnapshot = StatementSnapshot()

    init {
        // Four streams: the period's movement, and everything before it — the
        // latter collapsed to a single opening balance rather than printed.
        combine(
            invoiceDao.observeByCustomerRange(customerId, fromMillis, toMillis),
            paymentDao.observeByCustomerRange(customerId, fromMillis, toMillis),
            invoiceDao.observeByCustomerRange(customerId, 0L, fromMillis - 1),
            paymentDao.observeByCustomerRange(customerId, 0L, fromMillis - 1),
        ) { invoices, payments, priorInvoices, priorPayments ->
            StatementSnapshot(
                openingBalance = movementsOf(priorInvoices, priorPayments).sumOf { it.movement },
                movements = movementsOf(invoices, payments).sortedBy { it.createdAt },
                isLive = false,
            )
        }
            .onEach { snapshot ->
                local = snapshot
                if (live == null && !awaitingServer) publish(snapshot)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val customer = customers.findById(customerId)
            // Whoever is holding the phone is who the shopkeeper will come back
            // to about this paper, so the statement is signed with their name.
            val salesman = session.currentUserId?.let { users.findById(it) }
            _state.update {
                it.copy(
                    customerNameAr = customer?.nameAr.orEmpty(),
                    customerCode = customer?.code.orEmpty(),
                    customerPhone = customer?.phone.orEmpty(),
                    salesmanNameAr = salesman?.nameAr.orEmpty(),
                )
            }
            // The whole account, if it can be reached. Room has only what this
            // handset created, and a statement short of another van's invoices
            // prints a debt the shopkeeper will not recognise.
            val snapshot = statements.load(
                customerNumber = customer?.code.orEmpty(),
                customerId = customerId,
                fromMillis = fromMillis,
                toMillis = toMillis,
            )
            live = snapshot
            awaitingServer = false
            publish(snapshot ?: local)
        }

        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val info = companyInfo.getForPrint()
            _state.update {
                it.copy(
                    companyNameAr = info.nameAr,
                    companyNameEn = info.nameEn,
                    companyTaxNumber = info.taxNumber,
                    companyLogo = info.logo,
                )
            }
        }
    }

    private fun publish(snapshot: StatementSnapshot) {
        var running = snapshot.openingBalance
        val rows = snapshot.movements.map { m ->
            running += m.movement
            StatementRow(
                createdAt = m.createdAt,
                number = m.number,
                docType = when (m.docType) {
                    StatementDocType.SALE -> CustomerStatement.TYPE_SALE
                    StatementDocType.RETURN -> CustomerStatement.TYPE_RETURN
                    StatementDocType.PAYMENT -> DOC_PAYMENT
                },
                method = m.method,
                debit = m.debit,
                credit = m.credit,
                balance = running,
            )
        }
        _state.update {
            it.copy(
                isLoading = false,
                openingBalance = snapshot.openingBalance,
                rows = rows,
                isLocalOnly = !snapshot.isLive,
            )
        }
    }

    // What belongs on the ledger, and what it does to the balance, is [CustomerStatement]
    // — the same rule the on-screen statement uses. It used to be a copy of it here, and
    // the copy had drifted: an order moved this paper's balance by its full value while
    // moving the screen's by nothing.

    private fun movementsOf(
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
    ): List<StatementMovement> = buildList {
        invoices.filter(CustomerStatement::isLedgerEntry).forEach { inv ->
            val docType = when (inv.type) {
                CustomerStatement.TYPE_SALE -> StatementDocType.SALE
                CustomerStatement.TYPE_RETURN -> StatementDocType.RETURN
                // Moves no receivable, so it cannot be printed as a debit or a
                // credit — see the default in CustomerStatement.movement.
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

    fun onEvent(event: StatementPrintEvent) {
        when (event) {
            StatementPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }

            StatementPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }

            is StatementPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }

            is StatementPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }

            is StatementPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }

            StatementPrintEvent.RefreshDevices -> refreshDevices()
            StatementPrintEvent.Connect -> connect()
            StatementPrintEvent.Disconnect -> printer.disconnect()
            is StatementPrintEvent.Print -> print(event.statementPng)
            StatementPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
        }
    }

    private fun refreshDevices() {
        val devices = when (_state.value.connectType) {
            PrinterType.BLUETOOTH -> printer.discoverBluetooth()
            PrinterType.USB -> printer.discoverUsb()
            PrinterType.SERIAL -> printer.discoverSerialPorts()
            PrinterType.NETWORK -> emptyList()
        }
        _state.update { it.copy(discoveredDevices = devices) }
    }

    private fun connect() {
        val s = _state.value
        if (s.connectAddress.isBlank()) return
        val target = PrinterTarget(
            type = s.connectType,
            address = s.connectAddress,
            name = s.connectAddress,
            baudRate = printer.lastTarget?.baudRate ?: 115200,
        )
        viewModelScope.launch {
            when (val result = printer.connect(target)) {
                is PrintResult.Success -> _state.update { it.copy(showConnectDialog = false) }
                is PrintResult.Failure -> _state.update { it.copy(printMessageAr = result.message) }
            }
        }
    }

    /**
     * The statement goes to the printer as an IMAGE, like every other receipt in
     * this app: printer firmware cannot shape Arabic, so text nodes would come
     * out as disconnected letters in the wrong order.
     */
    private fun print(png: ByteArray) {
        if (_state.value.printerState !is PrinterState.Connected) return
        _state.update { it.copy(isPrinting = true, pendingPrint = false, printMessageAr = null) }
        viewModelScope.launch {
            val result = printer.printImage(png, PaperWidth.MM80)
            _state.update {
                it.copy(
                    isPrinting = false,
                    printMessageAr = when (result) {
                        is PrintResult.Success -> SUCCESS_MESSAGE
                        is PrintResult.Failure -> result.message
                    },
                )
            }
        }
    }

    private companion object {
        const val SUCCESS_MESSAGE = "تمت الطباعة بنجاح"
        /** [StatementRow.docType] for a receipt; vouchers use CustomerStatement's names. */
        const val DOC_PAYMENT = "PAYMENT"
    }
}
