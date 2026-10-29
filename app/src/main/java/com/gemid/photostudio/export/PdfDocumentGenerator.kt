package com.gemid.photostudio.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gemid.photostudio.sheet.PrintPaperDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportResult(
    val uri: Uri,
    val localFile: File,
    val fileName: String,
    val fileSizeBytes: Long,
    val widthPoints: Int,
    val heightPoints: Int,
    val paper: PrintPaperDefinition
)

object PdfDocumentGenerator {

    private const val POINTS_PER_INCH = 72f

    /**
     * Converts a physical paper definition into standard PDF PostScript points (1/72 inch).
     */
    fun getPagePoints(paper: PrintPaperDefinition, isLandscape: Boolean): Pair<Int, Int> {
        val wInches = if (isLandscape) maxOf(paper.widthInches, paper.heightInches) else minOf(paper.widthInches, paper.heightInches)
        val hInches = if (isLandscape) minOf(paper.widthInches, paper.heightInches) else maxOf(paper.widthInches, paper.heightInches)

        val wPoints = (wInches * POINTS_PER_INCH).toInt()
        val hPoints = (hInches * POINTS_PER_INCH).toInt()
        return Pair(wPoints, hPoints)
    }

    /**
     * Generates a calibrated physical scale PDF document containing the print sheet bitmap.
     */
    suspend fun generatePdf(
        context: Context,
        sheetBitmap: Bitmap,
        paper: PrintPaperDefinition,
        isLandscape: Boolean
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val (widthPoints, heightPoints) = getPagePoints(paper, isLandscape)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "GEMID_PRINT_${paper.id.uppercase()}_${timestamp}.pdf"

        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val localFile = File(exportDir, fileName)

        // 1. Create native Android PdfDocument
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(widthPoints, heightPoints, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas

        // 2. Draw high-res bitmap mapped precisely to physical page boundary
        val srcRect = Rect(0, 0, sheetBitmap.width, sheetBitmap.height)
        val dstRect = Rect(0, 0, widthPoints, heightPoints)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        canvas.drawBitmap(sheetBitmap, srcRect, dstRect, paint)

        // 3. Complete and write PDF
        document.finishPage(page)

        FileOutputStream(localFile).use { fos ->
            document.writeTo(fos)
            fos.flush()
        }
        document.close()

        // 4. Save to Android MediaStore Documents/Downloads for user file manager access
        var mediaStoreUri: Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/GemIDPhotoStudio")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                mediaStoreUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

                if (mediaStoreUri != null) {
                    resolver.openOutputStream(mediaStoreUri)?.use { os ->
                        localFile.inputStream().use { it.copyTo(os) }
                        os.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(mediaStoreUri, contentValues, null, null)
                }
            }
        } catch (_: Exception) {
            // MediaStore optional on older devices, local file remains
        }

        // 5. FileProvider URI for system sharing & printing
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            localFile
        )

        PdfExportResult(
            uri = mediaStoreUri ?: shareUri,
            localFile = localFile,
            fileName = fileName,
            fileSizeBytes = localFile.length(),
            widthPoints = widthPoints,
            heightPoints = heightPoints,
            paper = paper
        )
    }
}
