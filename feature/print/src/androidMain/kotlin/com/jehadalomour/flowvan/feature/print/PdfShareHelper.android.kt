package com.jehadalomour.flowvan.feature.print

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private class AndroidPdfShareHelper(private val context: Context) : PdfShareHelper {

    override suspend fun shareAsPdf(imageBitmap: ImageBitmap, invoiceNumber: String, a4: Boolean) {
        val bitmap = imageBitmap.asAndroidBitmap()
        val pdfFile = writePdf(bitmap, "invoice_${sanitize(invoiceNumber)}.pdf", a4)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    private fun writePdf(bitmap: Bitmap, fileName: String, a4: Boolean): File {
        // GraphicsLayer returns a hardware-backed bitmap; PDF canvas needs software rendering.
        val soft = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val document = PdfDocument()
        if (a4) {
            // Portrait A4 in PostScript points (1/72"): 595×842. The captured image is
            // high-res; drawing it scaled into the A4 rect embeds it crisply. Fit within a
            // margin, preserving aspect, centred horizontally and top-aligned.
            val a4w = 595
            val a4h = 842
            val margin = 24f
            val availW = a4w - margin * 2
            val availH = a4h - margin * 2
            val scale = minOf(availW / soft.width, availH / soft.height)
            val drawW = soft.width * scale
            val drawH = soft.height * scale
            val left = margin + (availW - drawW) / 2f
            val dest = android.graphics.RectF(left, margin, left + drawW, margin + drawH)
            val pageInfo = PdfDocument.PageInfo.Builder(a4w, a4h, 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(soft, null, dest, null)
            document.finishPage(page)
        } else {
            val pageInfo = PdfDocument.PageInfo.Builder(soft.width, soft.height, 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(soft, 0f, 0f, null)
            document.finishPage(page)
        }
        val file = File(context.cacheDir, fileName)
        document.writeTo(FileOutputStream(file))
        document.close()
        if (soft !== bitmap) soft.recycle()
        return file
    }

    private fun sanitize(name: String) = name.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
}

@Composable
actual fun rememberPdfShareHelper(): PdfShareHelper {
    val context = LocalContext.current
    return remember { AndroidPdfShareHelper(context) }
}
