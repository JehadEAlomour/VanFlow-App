package com.jehadalomour.flowvan.shared.presentation.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.PaymentDao
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class AllPaymentMethodFilter { ALL, CASH, CHEQUE, TRANSFER }

data class AllPaymentsReportState(
    val from: Long = 0L,
    val to: Long = 0L,
    val methodFilter: AllPaymentMethodFilter = AllPaymentMethodFilter.ALL,
    val payments: List<PaymentEntity> = emptyList(),
    val total: Double = 0.0,
    val cashTotal: Double = 0.0,
    val chequeTotal: Double = 0.0,
    val transferTotal: Double = 0.0,
    val count: Int = 0,
)

class AllPaymentsReportViewModel(private val paymentDao: PaymentDao) : ViewModel() {

    private val _from = MutableStateFlow(0L)
    private val _to = MutableStateFlow(0L)
    private val _methodFilter = MutableStateFlow(AllPaymentMethodFilter.ALL)

    private val _state = MutableStateFlow(AllPaymentsReportState())
    val state: StateFlow<AllPaymentsReportState> = _state.asStateFlow()

    init {
        initDefaults()
        observe()
    }

    @OptIn(ExperimentalTime::class)
    private fun initDefaults() {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val from = today.atStartOfDayIn(tz).toEpochMilliseconds()
        _from.value = from; _to.value = nowMs
        _state.update { it.copy(from = from, to = nowMs) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_from, _to, _methodFilter) { f, t, m -> Triple(f, t, m) }
            .flatMapLatest { (f, t, method) ->
                if (method == AllPaymentMethodFilter.ALL) paymentDao.observeAllByRange(f, t)
                else paymentDao.observeAllByMethodAndRange(method.name, f, t)
            }
            .onEach { list ->
                val allPayments = if (_methodFilter.value == AllPaymentMethodFilter.ALL) list
                else list // already filtered by method
                _state.update {
                    it.copy(
                        from = _from.value, to = _to.value, methodFilter = _methodFilter.value,
                        payments = list,
                        total = list.sumOf { p -> p.amount },
                        cashTotal = list.filter { p -> p.method == "CASH" }.sumOf { p -> p.amount },
                        chequeTotal = list.filter { p -> p.method == "CHEQUE" }.sumOf { p -> p.amount },
                        transferTotal = list.filter { p -> p.method == "TRANSFER" }.sumOf { p -> p.amount },
                        count = list.size,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(ms: Long) { _from.value = ms; _state.update { it.copy(from = ms) } }
    fun setTo(ms: Long) { _to.value = ms; _state.update { it.copy(to = ms) } }
    fun setMethodFilter(f: AllPaymentMethodFilter) { _methodFilter.value = f }
}
