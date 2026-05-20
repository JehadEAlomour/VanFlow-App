package com.jehadalomour.flowvan.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

interface PdfShareHelper {
    suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String)
    suspend fun printDocument(imageBitmap: ImageBitmap, invoiceNumber: String)
}

@Composable
expect fun rememberPdfShareHelper(): PdfShareHelper
