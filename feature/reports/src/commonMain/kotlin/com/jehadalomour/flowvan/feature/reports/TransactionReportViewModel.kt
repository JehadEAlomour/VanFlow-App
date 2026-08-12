package com.jehadalomour.flowvan.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.TransactionReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
 * تقرير الحركات — every movement on one customer's account in a date range,
 * read from the SERVER: sales, returns, orders and collections together, with
 * the cash/credit split the local database cannot answer.
 */
class TransactionReportViewModel(
    private val customerId: String,
    private val customers: CustomerRepository,
    private val reports: TransactionReportRepository,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(
        TransactionReportState(
            customerId = customerId,
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        ),
    )
    val state: StateFlow<TransactionReportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val customer = customers.findById(customerId)
            _state.update {
                it.copy(
                    customerNumber = customer?.code.orEmpty(),
                    customerNameAr = customer?.nameAr.orEmpty(),
                )
            }
            load()
        }
    }

    fun onEvent(event: TransactionReportEvent) {
        when (event) {
            is TransactionReportEvent.TypeFilterChanged ->
                // Filtering is local to what is already loaded — the chip must not
                // cost a round trip on a van's connection.
                _state.update { it.copy(typeFilter = event.filter) }

            is TransactionReportEvent.DateRangeChanged -> {
                _state.update {
                    it.copy(fromMillis = event.fromMillis, toMillis = event.toMillis)
                }
                load()
            }

            TransactionReportEvent.Retry -> load()
        }
    }

    private fun load() {
        val s = _state.value
        if (s.customerNumber.isBlank()) return
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorAr = null) }
        loadJob = viewModelScope.launch {
            val report = reports.load(
                customerNumber = s.customerNumber,
                customerId = customerId,
                from = s.fromMillis.toIsoDate(),
                to = s.toMillis.toIsoDate(),
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    report = report,
                    // An empty report is a real answer; an unreachable one is not,
                    // and the two must never look the same on this screen.
                    errorAr = if (report.isLive) null else ERR_OFFLINE,
                )
            }
        }
    }

    private companion object {
        const val ERR_OFFLINE =
            "تعذّر جلب الحركات من الخادم. هذا التقرير يعرض حركات العميل كاملة، لذلك يحتاج اتصالاً بالإنترنت."
    }
}

@OptIn(ExperimentalTime::class)
private fun Long.toIsoDate(): String {
    val d = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${d.year}-${d.monthNumber.toString().padStart(2, '0')}-" +
        d.dayOfMonth.toString().padStart(2, '0')
}

@OptIn(ExperimentalTime::class)
private fun startOfMonthMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        .toLocalDateTime(tz).date
    return LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
private fun endOfTodayMillis(): Long {
    val tz = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        .toLocalDateTime(tz).date
    return today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds() - 1
}
