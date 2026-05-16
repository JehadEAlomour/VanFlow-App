package com.jehadalomour.flowvan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

private class IosPdfShareHelper : PdfShareHelper {
    override suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String) {
        // TODO: implement iOS PDF share via UIActivityViewController
    }
    override suspend fun printDocument(imageBitmap: ImageBitmap, invoiceNumber: String) {
        // TODO: implement iOS print via UIPrintInteractionController
    }
}

@Composable
actual fun rememberPdfShareHelper(): PdfShareHelper = remember { IosPdfShareHelper() }
