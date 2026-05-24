package com.jehadalomour.flowvan.platform.printer

import androidx.compose.ui.graphics.ImageBitmap

/** Encode a captured Compose [ImageBitmap] to PNG bytes for handing to a ViewModel / printer. */
expect fun ImageBitmap.toPngBytes(): ByteArray
