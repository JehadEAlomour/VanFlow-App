package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.model.DailyKpi
import com.jehadalomour.flowvan.core.model.Shift

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
