package com.jehadalomour.flowvan.core.network.http

import kotlin.math.abs
import kotlin.math.roundToLong

/** Backend money is integer fils (1 JOD = 1000 fils). Convert at the mapper boundary only. */
fun Int.filsToJod(): Double = this / 1000.0

fun Long.filsToJod(): Double = this / 1000.0

fun Double.jodToFils(): Int = (this * 1000.0).roundToLong().toInt()

/** Numeric-string money fields ("1.250") returned by vouchers/vendors/warehouses. */
fun String.numericStringToDouble(): Double = trim().toDoubleOrNull() ?: 0.0

/** Format a JOD/qty value as a 3-decimal numeric string ("1.250") for the voucher API. */
fun Double.toAmountString(): String {
    val milli = (this * 1000).roundToLong()
    val sign = if (milli < 0) "-" else ""
    val a = abs(milli)
    return "$sign${a / 1000}.${(a % 1000).toString().padStart(3, '0')}"
}

/** Format a rate (0.16) as a whole-number percent string ("16"). */
fun Double.toPercentString(): String = (this * 100).roundToLong().toString()
