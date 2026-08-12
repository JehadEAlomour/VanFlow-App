package com.jehadalomour.flowvan.core.common.format

import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Format a JOD amount with 3 decimals, never rounded to 2.
 * Example: 123.45 -> "123.450 د.أ" (AR) or "123.450 JOD" (EN)
 */
fun Double.formatJod(language: AppLanguage = AppLanguage.AR): String {
    val suffix = if (language == AppLanguage.AR) "د.أ" else "JOD"
    return "${formatAmount()} $suffix"
}

/**
 * The same figure without the currency word — for captions that already sit
 * beside an amount carrying it, where repeating "د.أ" costs width and says
 * nothing. Example: 123.45 -> "123.450".
 */
fun Double.formatAmount(): String {
    val sign = if (this < 0) "-" else ""
    val absVal = abs(this)
    val whole = absVal.toLong()
    val frac = ((absVal - whole) * 1000).roundToLong()
    val (w, f) = if (frac >= 1000) (whole + 1) to 0L else whole to frac
    return "$sign$w.${f.toString().padStart(3, '0')}"
}
