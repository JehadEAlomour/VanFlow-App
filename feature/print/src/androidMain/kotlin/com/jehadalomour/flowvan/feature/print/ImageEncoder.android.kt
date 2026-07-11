package com.jehadalomour.flowvan.feature.print

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

actual fun ByteArray.toImageBitmapOrNull(): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap() }.getOrNull()
