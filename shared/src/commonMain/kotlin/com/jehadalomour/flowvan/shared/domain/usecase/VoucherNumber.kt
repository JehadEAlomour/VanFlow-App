package com.jehadalomour.flowvan.shared.domain.usecase

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object VoucherNumber {
    /**
     * Format: PREFIX-YYYYMMDD-####
     * The day-sequence portion is derived from a millisecond-of-day fraction so it's
     * monotonic without needing a counter table; collisions across two devices are
     * resolved by the backend in M17.
     */
    @OptIn(ExperimentalTime::class)
    fun next(prefix: String): String {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val ldt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
        val ymd = "${ldt.year}${ldt.monthNumber.pad2()}${ldt.dayOfMonth.pad2()}"
        val secondOfDay = ldt.hour * 3600 + ldt.minute * 60 + ldt.second
        val seq = (secondOfDay % 10000).toString().padStart(4, '0')
        return "$prefix-$ymd-$seq"
    }

    private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
}
