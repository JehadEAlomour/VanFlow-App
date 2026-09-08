package com.jehadalomour.flowvan.feature.print

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

private class IosPdfShareHelper : PdfShareHelper {
    override suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String, a4: Boolean) {
        // TODO: implement iOS PDF share via UIActivityViewController
    }

    override suspend fun shareAsPagedPdf(pages: List<ImageBitmap>, documentName: String) {
        // TODO: implement iOS PDF share via UIActivityViewController — same gap as
        // shareAsPdf above, not a separate one.
    }
}

@Composable
actual fun rememberPdfShareHelper(): PdfShareHelper = remember { IosPdfShareHelper() }
