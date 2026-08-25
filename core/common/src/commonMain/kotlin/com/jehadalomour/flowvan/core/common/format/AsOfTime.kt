package com.jehadalomour.flowvan.core.common.format

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A compact, unambiguous "as of" stamp for a cached figure — "2026-08-25 14:30"
 * in the device timezone. LTR/mono by nature (digits + separators), so it reads
 * the same in Arabic and English; callers add the "كما في / as of" label.
 */
fun formatAsOf(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val mm = dt.monthNumber.pad2()
    val dd = dt.dayOfMonth.pad2()
    val hh = dt.hour.pad2()
    val min = dt.minute.pad2()
    return "${dt.year}-$mm-$dd $hh:$min"
}

private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
