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
 * التقرير المفصل للحركات — every voucher in the range with its item lines.
 *
 * The same server data as the flat report, one level deeper: what the customer
 * took, not merely what it cost. That is the report a rep opens when the shop
 * disputes a delivery rather than a balance.
 */
class DetailedTxnReportViewModel(
    private val customerId: String,
    private val customers: CustomerRepository,
    private val reports: TransactionReportRepository,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(
        DetailedTxnReportState(
            customerId = customerId,
            fromMillis = startOfMonthMillis(),
            toMillis = endOfTodayMillis(),
        ),
    )
    val state: StateFlow<DetailedTxnReportState> = _state.asStateFlow()

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

    fun onEvent(event: DetailedTxnReportEvent) {
        when (event) {
            is DetailedTxnReportEvent.DateRangeChanged -> {
                _state.update {
                    // Expansions are dropped with the data they referred to;
                    // keeping them would re-open unrelated vouchers by id collision.
                    it.copy(
                        fromMillis = event.fromMillis,
                        toMillis = event.toMillis,
                        expanded = emptySet(),
                    )
                }
                load()
            }

            is DetailedTxnReportEvent.ToggleExpanded -> _state.update { s ->
                s.copy(
                    expanded = if (event.docId in s.expanded) s.expanded - event.docId
                    else s.expanded + event.docId,
                )
            }

            DetailedTxnReportEvent.ExpandAll -> _state.update { s ->
                s.copy(expanded = s.report.docs.map { it.id }.toSet())
            }

            DetailedTxnReportEvent.CollapseAll -> _state.update { it.copy(expanded = emptySet()) }

            DetailedTxnReportEvent.Retry -> load()
        }
    }

    private fun load() {
        val s = _state.value
        if (s.customerNumber.isBlank()) return
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorAr = null) }
        loadJob = viewModelScope.launch {
            val report = reports.loadDetailed(
                customerNumber = s.customerNumber,
                customerId = customerId,
                from = s.fromMillis.toIsoDate(),
                to = s.toMillis.toIsoDate(),
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    report = report,
                    errorAr = if (report.isLive) null else ERR_OFFLINE,
                )
            }
        }
    }

    private companion object {
        const val ERR_OFFLINE =
            "تعذّر جلب الحركات من الخادم. التقرير المفصل يعرض بنود كل سند، لذلك يحتاج اتصالاً بالإنترنت."
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
