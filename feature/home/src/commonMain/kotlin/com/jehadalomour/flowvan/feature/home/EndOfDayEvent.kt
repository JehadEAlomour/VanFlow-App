package com.jehadalomour.flowvan.feature.home

sealed class EndOfDayEvent {
    data object OpenConfirmDialog : EndOfDayEvent()
    data object DismissConfirmDialog : EndOfDayEvent()
    data object ConfirmEndShift : EndOfDayEvent()
}
