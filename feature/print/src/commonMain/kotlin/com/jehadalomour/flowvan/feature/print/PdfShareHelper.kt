package com.jehadalomour.flowvan.feature.print

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Turning a captured receipt into something that leaves the phone.
 *
 * Sharing only. The OS print dialog used to live here too and was removed: in
 * the field a rep prints to the thermal roll on their belt, and nothing else —
 * a system print target means a networked office printer they are nowhere near.
 * The office prints from the dashboard, not from a handset.
 */
interface PdfShareHelper {
    suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String)
}

@Composable
expect fun rememberPdfShareHelper(): PdfShareHelper
