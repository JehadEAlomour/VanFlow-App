package com.jehadalomour.flowvan.feature.print

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Encode a captured Compose [ImageBitmap] to PNG bytes for handing to a ViewModel / printer. */
expect fun ImageBitmap.toPngBytes(): ByteArray

/** Decode raw image bytes (PNG/JPEG/…) into an [ImageBitmap], or null if they aren't a valid image. */
expect fun ByteArray.toImageBitmapOrNull(): ImageBitmap?

/**
 * Decode a company logo held as a `data:<mime>;base64,...` URI (or bare base64) into an
 * [ImageBitmap]. Returns null for blank / a plain URL / undecodable data, so callers fall
 * back to the bundled default logo. Used to print the company's own logo on the receipt.
 */
@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64Image(data: String): ImageBitmap? {
    if (data.isBlank()) return null
    val b64 = data.substringAfter("base64,", data).trim()
    val bytes = runCatching { Base64.decode(b64) }.getOrNull() ?: return null
    return bytes.toImageBitmapOrNull()
}
