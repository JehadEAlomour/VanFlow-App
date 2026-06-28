package com.jehadalomour.flowvan.feature.print

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.toPngBytes(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
        ?: ByteArray(0)
