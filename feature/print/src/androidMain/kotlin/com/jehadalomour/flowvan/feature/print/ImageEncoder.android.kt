package com.jehadalomour.flowvan.feature.print

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.toPngBytes(): ByteArray {
    val bitmap = asAndroidBitmap()
    // GraphicsLayer captures can be hardware-backed; PNG compression needs the pixels.
    val soft = bitmap.toSoftwareBitmap()
    return ByteArrayOutputStream().use { stream ->
        soft.compress(Bitmap.CompressFormat.PNG, 100, stream)
        if (soft !== bitmap) soft.recycle()
        stream.toByteArray()
    }
}

actual fun ByteArray.toImageBitmapOrNull(): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap() }.getOrNull()
