package com.jehadalomour.flowvan.shared.presentation.feature.endofday

import com.jehadalomour.flowvan.shared.domain.model.DailyKpi
import com.jehadalomour.flowvan.shared.domain.model.Shift

data class EndOfDayState(
    val kpi: DailyKpi? = null,
    val cashCollectedToday: Double = 0.0,
    val chequesCollectedToday: Double = 0.0,
    val transfersCollectedToday: Double = 0.0,
    val unsyncedInvoices: Int = 0,
    val unsyncedPayments: Int = 0,
    val activeShift: Shift? = null,
    val showConfirmDialog: Boolean = false,
    val isEnding: Boolean = false,
    val done: Boolean = false,
)
