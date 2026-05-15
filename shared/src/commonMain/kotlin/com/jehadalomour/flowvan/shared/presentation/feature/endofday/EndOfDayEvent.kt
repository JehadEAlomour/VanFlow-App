package com.jehadalomour.flowvan.shared.presentation.feature.endofday

sealed class EndOfDayEvent {
    data object OpenConfirmDialog : EndOfDayEvent()
    data object DismissConfirmDialog : EndOfDayEvent()
    data object ConfirmEndShift : EndOfDayEvent()
}
