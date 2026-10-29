package com.gemid.photostudio.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.gemid.photostudio.sheet.PrintPaperDefinition
import java.io.FileOutputStream
import java.io.IOException

object WifiPrintHelper {

    /**
     * Sends the composite print sheet bitmap directly to connected Wi-Fi printers
     * (Epson, Canon SELPHY, HiTi, Mopria, HP) using Android's native PrintManager.
     */
    fun printSheet(
        context: Context,
        sheetBitmap: Bitmap,
        paper: PrintPaperDefinition,
        isLandscape: Boolean,
        jobTitle: String = "GemID Photo Sheet ${paper.dimensionInches}"
    ): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false

        // Configure default media attributes matching paper size and 300 DPI
        val mediaSize = when (paper) {
            PrintPaperDefinition.SIZE_4X6 -> {
                if (isLandscape) PrintAttributes.MediaSize.NA_INDEX_4X6.asLandscape()
                else PrintAttributes.MediaSize.NA_INDEX_4X6.asPortrait()
            }
            PrintPaperDefinition.SIZE_5X7 -> {
                if (isLandscape) PrintAttributes.MediaSize("na_5x7_landscape", "5×7 in", 7000, 5000)
                else PrintAttributes.MediaSize("na_5x7_portrait", "5×7 in", 5000, 7000)
            }
            PrintPaperDefinition.SIZE_A4 -> {
                if (isLandscape) PrintAttributes.MediaSize.ISO_A4.asLandscape()
                else PrintAttributes.MediaSize.ISO_A4.asPortrait()
            }
            PrintPaperDefinition.SIZE_LETTER -> {
                if (isLandscape) PrintAttributes.MediaSize.NA_LETTER.asLandscape()
                else PrintAttributes.MediaSize.NA_LETTER.asPortrait()
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setResolution(PrintAttributes.Resolution("300dpi", "300 DPI Studio Lab Quality", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val adapter = BitmapPrintDocumentAdapter(
            context = context,
            bitmap = sheetBitmap,
            jobTitle = jobTitle
        )

        printManager.print(jobTitle, adapter, printAttributes)
        return true
    }

    private class BitmapPrintDocumentAdapter(
        private val context: Context,
        private val bitmap: Bitmap,
        private val jobTitle: String
    ) : PrintDocumentAdapter() {

        private var printedPdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            printedPdfDocument = PrintedPdfDocument(context, newAttributes)

            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            val info = PrintDocumentInfo.Builder("$jobTitle.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val pdfDocument = printedPdfDocument ?: run {
                callback.onWriteFailed("PdfDocument not initialized")
                return
            }

            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }

            try {
                val page = pdfDocument.startPage(0)
                val canvas = page.canvas

                // Draw bitmap filling page bounds
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val dstRect = Rect(0, 0, canvas.width, canvas.height)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                pdfDocument.finishPage(page)

                FileOutputStream(destination.fileDescriptor).use { fos ->
                    pdfDocument.writeTo(fos)
                    fos.flush()
                }

                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument.close()
                printedPdfDocument = null
            }
        }
    }
}
