package com.jehadalomour.flowvan.feature.print

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.toPngBytes(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
        ?: ByteArray(0)

actual fun ByteArray.toImageBitmapOrNull(): ImageBitmap? =
    runCatching { Image.makeFromEncoded(this).toComposeImageBitmap() }.getOrNull()
