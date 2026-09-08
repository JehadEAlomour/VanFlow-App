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
    /**
     * Share a captured document as a PDF. When [a4] is true the image is placed on a
     * portrait A4 page (595×842 pt), scaled to fit with a small margin — for the account
     * statement and the voucher A4 document. When false the page matches the image
     * (the thermal-receipt behaviour), unchanged.
     */
    suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String, a4: Boolean = false)
}

@Composable
expect fun rememberPdfShareHelper(): PdfShareHelper
