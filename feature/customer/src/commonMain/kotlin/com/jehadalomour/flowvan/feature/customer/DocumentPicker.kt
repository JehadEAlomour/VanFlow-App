package com.jehadalomour.flowvan.feature.customer

import androidx.compose.runtime.Composable

/**
 * The customer document photo a salesman must attach — the shop's registration,
 * the owner's ID, whatever the office asked for.
 *
 * Two sources, because both are real in the field: the rep photographs the paper
 * on the spot (CAMERA), or picks one they already took while their hands were
 * full (GALLERY).
 *
 * Returns the raw bytes rather than a file path or a platform URI: the caller
 * uploads them, and a URI would need a permission grant that outlives the picker
 * on Android and means nothing at all on iOS.
 */
interface DocumentPicker {
    /** Opens the system camera. Null when the rep backs out. */
    suspend fun capture(): PickedDocument?

    /** Opens the system photo picker. Null when the rep backs out. */
    suspend fun pickFromGallery(): PickedDocument?
}

data class PickedDocument(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
) {
    // ByteArray gives data classes reference equality, which silently breaks
    // Compose recomposition checks and any equals() the caller relies on.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedDocument) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int =
        (bytes.contentHashCode() * 31 + fileName.hashCode()) * 31 + mimeType.hashCode()
}

@Composable
expect fun rememberDocumentPicker(): DocumentPicker
