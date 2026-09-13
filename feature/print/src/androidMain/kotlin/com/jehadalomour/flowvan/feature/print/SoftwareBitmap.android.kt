package com.jehadalomour.flowvan.feature.print

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * A bitmap that can actually be read back — PNG compression and a PDF canvas both
 * need the pixels, and a hardware-backed bitmap has none on the Java side.
 *
 * WHY THIS IS NOT JUST `config == Bitmap.Config.HARDWARE`. That constant arrived in
 * API 26, and this app runs down to API 24 — a Sunmi T2 is Android 7.1, API 25.
 * Reading it there throws `NoSuchFieldError`, an Error rather than an Exception, at
 * the moment the receipt is captured. Nothing on the print path catches it, so the
 * whole app went down every single time a rep pressed print on a Sunmi, while the
 * same code was flawless on every newer handset it was tested on.
 *
 * The version check has to sit in front of a SEPARATE method, not merely in front
 * of the comparison. ART resolves a field the first time its instruction runs, so
 * keeping the reference out of a method the old device ever calls is what makes it
 * safe; an `if` in the same method leaves the instruction there to be reached.
 *
 * Returns the receiver untouched when it is already a software bitmap, so callers
 * must compare identity before recycling — recycling the original would blank the
 * very bitmap the caller is still drawing from.
 */
internal fun Bitmap.toSoftwareBitmap(): Bitmap =
    if (isHardwareBacked()) copy(Bitmap.Config.ARGB_8888, false) else this

private fun Bitmap.isHardwareBacked(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasHardwareConfig()

@RequiresApi(Build.VERSION_CODES.O)
private fun Bitmap.hasHardwareConfig(): Boolean = config == Bitmap.Config.HARDWARE
