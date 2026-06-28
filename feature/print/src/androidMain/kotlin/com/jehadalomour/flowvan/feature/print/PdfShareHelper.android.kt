package com.jehadalomour.flowvan.feature.print

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private class AndroidPdfShareHelper(private val context: Context) : PdfShareHelper {

    override suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String) {
        val bitmap = imageBitmap.asAndroidBitmap()
        val pdfFile = writePdf(bitmap, "invoice_${sanitize(invoiceNumber)}.pdf")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    override suspend fun printDocument(imageBitmap: ImageBitmap, invoiceNumber: String) {
        val bitmap = imageBitmap.asAndroidBitmap()
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "invoice_${sanitize(invoiceNumber)}"
        printManager.print(jobName, BitmapPrintDocumentAdapter(bitmap, jobName), null)
    }

    private fun writePdf(bitmap: Bitmap, fileName: String): File {
        // GraphicsLayer returns a hardware-backed bitmap; PDF canvas needs software rendering.
        val soft = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(soft.width, soft.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(soft, 0f, 0f, null)
        document.finishPage(page)
        val file = File(context.cacheDir, fileName)
        document.writeTo(FileOutputStream(file))
        document.close()
        if (soft !== bitmap) soft.recycle()
        return file
    }

    private fun sanitize(name: String) = name.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
}

private class BitmapPrintDocumentAdapter(
    private val bitmap: Bitmap,
    private val jobName: String,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val soft = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(soft.width, soft.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(soft, 0f, 0f, null)
        document.finishPage(page)
        document.writeTo(FileOutputStream(destination.fileDescriptor))
        document.close()
        if (soft !== bitmap) soft.recycle()
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }
}

@Composable
actual fun rememberPdfShareHelper(): PdfShareHelper {
    val context = LocalContext.current
    return remember { AndroidPdfShareHelper(context) }
}
