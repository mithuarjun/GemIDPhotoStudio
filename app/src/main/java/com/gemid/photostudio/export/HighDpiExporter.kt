package com.gemid.photostudio.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val uri: Uri,
    val localFile: File,
    val mimeType: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val dpi: Int = 300
)

enum class ExportFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png")
}

object HighDpiExporter {

    /**
     * Exports a 300 DPI composite print sheet bitmap to storage with injected EXIF 300 DPI metadata.
     */
    suspend fun exportImage(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        qualityPercent: Int = 100,
        dpi: Int = 300
    ): ExportResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "GEMID_SHEET_${timestamp}.${format.extension}"

        // 1. Save to local app cache/files for internal sharing and EXIF manipulation
        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val localFile = File(exportDir, fileName)

        FileOutputStream(localFile).use { fos ->
            val compressFormat = if (format == ExportFormat.PNG) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(compressFormat, qualityPercent, fos)
            fos.flush()
        }

        // 2. Inject 300 DPI EXIF Metadata Header
        try {
            val exif = ExifInterface(localFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, "$dpi/1")
            exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, "$dpi/1")
            exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2") // 2 = Inches
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, bitmap.width.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, bitmap.height.toString())
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GemID Photo Studio Pro")
            exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date()))
            exif.saveAttributes()
        } catch (_: Exception) {
            // Non-fatal if EXIF injection is not supported by format
        }

        // 3. Save copy to Android MediaStore (Pictures/GemIDPhotoStudio) for device gallery access
        var mediaStoreUri: Uri? = null
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GemIDPhotoStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            mediaStoreUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (mediaStoreUri != null) {
                resolver.openOutputStream(mediaStoreUri)?.use { os: OutputStream ->
                    localFile.inputStream().use { input ->
                        input.copyTo(os)
                    }
                    os.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(mediaStoreUri, contentValues, null, null)
                }
            }
        } catch (_: Exception) {
            // MediaStore optional on restricted devices, local file remains
        }

        // 4. Create content URI for secure sharing
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            localFile
        )

        ExportResult(
            uri = mediaStoreUri ?: shareUri,
            localFile = localFile,
            mimeType = format.mimeType,
            fileName = fileName,
            fileSizeBytes = localFile.length(),
            width = bitmap.width,
            height = bitmap.height,
            dpi = dpi
        )
    }
}
