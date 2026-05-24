package com.jehadalomour.flowvan.platform.printer

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

@RequiresApi(Build.VERSION_CODES.O)
actual fun ImageBitmap.toPngBytes(): ByteArray {
    val bitmap = asAndroidBitmap()
    // GraphicsLayer captures are hardware-backed; PNG compression needs a software bitmap.
    val soft = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
    return ByteArrayOutputStream().use { stream ->
        soft.compress(Bitmap.CompressFormat.PNG, 100, stream)
        if (soft !== bitmap) soft.recycle()
        stream.toByteArray()
    }
}
