package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.isWithinProximity
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ErpFinanceRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.dto.LogVisitRequest
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    private val location: LocationProvider,
    private val erpFinance: ErpFinanceRepository,
    private val erpSync: ErpCustomerSync,
) : ViewModel() {

    /** This rep may only act on a customer while at its location (~1 km). */
    private val locationLocked = session.can("customers.requireProximity")

    private val _state = MutableStateFlow(
        CustomerDashboardState(
            requireVisitReason = session.can("customers.visitReason"),
            locationLocked = locationLocked,
        ),
    )
    val state: StateFlow<CustomerDashboardState> = _state.asStateFlow()
    private val log = Logger.withTag("CustomerVisit")

    init {
        // On every visit-open, seed a missing store pin from the rep's current GPS — the
        // rep is physically at the customer. Runs for ALL reps (not just proximity-locked
        // ones); locked reps additionally get the ~1 km proximity check.
        viewModelScope.launch {
            if (locationLocked) setUpProximity() else seedMissingLocation()
        }
        // Log the call the moment the rep JOINS the customer, not only when they
        // leave. A rep who backs out with the system gesture, loses signal, or has
        // the app killed never reaches the leave flow — and the visit vanished
        // with it, which is why a full selling day could report zero visits.
        //
        // Safe to send early because the server keeps one visit per rep, per
        // customer, per day and latches hadSale: this entry row is later upgraded
        // by the leave call, or by the voucher itself.
        logVisit(hadSale = false, note = null)

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

        // ERP balance (book of record): observe the offline cache for display, then
        // pull a fresh figure in the background. Offline → the cached row (with its
        // "as of" time) simply stays.
        erpFinance.observeCustomer(customerId)
            .onEach { row ->
                _state.update {
                    it.copy(
                        erpBalance = row?.balance,
                        erpAvailable = row?.available == true,
                        erpAsOfMillis = row?.asOfMillis ?: 0L,
                    )
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch { erpSync.refresh(customerId) }
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
            // Refresh the ERP balance/statement too (best-effort; keeps cache on failure).
            erpSync.refresh(customerId)
        }
    }

    /**
     * If the customer has no saved location, seed it from the rep's current GPS — the rep
     * is at the store, so this point IS the store. Seed-once server-side (only fills an
     * empty pin), so it's safe/idempotent. Returns the resolved pin (existing or freshly
     * seeded), or null when neither a pin nor a GPS fix is available. Runs on every open.
     */
    private suspend fun seedMissingLocation(): Pair<Double, Double>? {
        // Wait for the customer to load from the local cache.
        val customer = customers.observeById(customerId).filterNotNull().first()
        val cLat = customer.lat
        val cLng = customer.lng
        if (cLat != null && cLng != null) return cLat to cLng // already pinned — nothing to do

        val fix = location.lastLocation() ?: return null
        // Bootstrap the missing store location from the rep's position.
        val seeded = runCatching { customerApi.seedLocation(customerId, fix.lat, fix.lng) }
            .getOrElse {
                log.w("seedLocation failed (offline?): ${it.message}")
                null
            }
        return if (seeded != null) {
            customers.cacheAll(listOf(seeded.toEntity()))
            (seeded.latitude?.toDoubleOrNull() ?: fix.lat) to
                (seeded.longitude?.toDoubleOrNull() ?: fix.lng)
        } else {
            // Couldn't reach the server — the rep IS here, so use the live fix. The voucher
            // carries the coords and the backend seeds on sync.
            fix.lat to fix.lng
        }
    }

    /**
     * Location lock (customers.requireProximity). Seeds a missing pin (via
     * [seedMissingLocation]), then decides whether actions are allowed. Fail closed —
     * no GPS fix ⇒ blocked.
     */
    private suspend fun setUpProximity() {
        val fix = location.lastLocation()
        if (fix == null) {
            _state.update { it.copy(proximityBlock = ProximityBlock.NO_GPS) }
            return
        }
        val pin = seedMissingLocation() ?: (fix.lat to fix.lng)
        val within = isWithinProximity(fix, LatLng(pin.first, pin.second))
        _state.update {
            it.copy(proximityBlock = if (within) ProximityBlock.NONE else ProximityBlock.TOO_FAR)
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

    /**
     * Record the visit on the server so it reflects on the dashboard and the
     * visit reports.
     *
     * Called on entry (hadSale = false) and again on leave. The server merges
     * both into the day's single row, so calling it twice is not a double count
     * and a dropped call is not a lost visit.
     */
    private fun logVisit(hadSale: Boolean, note: String?) {
        val repId = session.currentRepId ?: return
        viewModelScope.launch {
            try {
                // Never throws by contract, but a null fix is normal indoors.
                val fix = location.lastLocation()
                customerApi.logVisit(
                    customerId,
                    LogVisitRequest(
                        repId = repId,
                        hadSale = hadSale,
                        visitNote = note,
                        lat = fix?.lat,
                        lng = fix?.lng,
                    ),
                )
            } catch (e: Exception) {
                log.w("logVisit failed: ${e.message}")
            }
        }
    }
}
