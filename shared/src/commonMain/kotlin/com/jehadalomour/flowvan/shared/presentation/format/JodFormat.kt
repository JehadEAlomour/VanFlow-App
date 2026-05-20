package com.jehadalomour.flowvan.shared.presentation.format

import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Format a JOD amount with 3 decimals, never rounded to 2.
 * Example: 123.45 -> "123.450 د.أ" (AR) or "123.450 JOD" (EN)
 */
fun Double.formatJod(language: AppLanguage = AppLanguage.AR): String {
    val sign = if (this < 0) "-" else ""
    val absVal = abs(this)
    val whole = absVal.toLong()
    val frac = ((absVal - whole) * 1000).roundToLong()
    val (w, f) = if (frac >= 1000) (whole + 1) to 0L else whole to frac
    val fracStr = f.toString().padStart(3, '0')
    val suffix = if (language == AppLanguage.AR) "د.أ" else "JOD"
    return "$sign$w.$fracStr $suffix"
}
