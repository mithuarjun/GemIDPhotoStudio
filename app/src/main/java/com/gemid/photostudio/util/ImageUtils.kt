package com.gemid.photostudio.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProcessedPhotoResult(
    val fileUri: Uri,
    val filePath: String,
    val bitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
    val processedWidth: Int,
    val processedHeight: Int,
    val rotationDegrees: Int,
    val fileSizeBytes: Long
)

object ImageUtils {

    private const val MAX_DIMENSION = 3000 // High-res passport photo standard (~300 DPI for 4x6+ sheets)

    /**
     * Creates a new temporary file in app storage for CameraX capture output.
     */
    fun createCaptureFile(context: Context): File {
        val storageDir = File(context.cacheDir, "captures").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(storageDir, "GEMID_IMG_${timestamp}.jpg")
    }

    /**
     * Decodes an image from URI, automatically corrects EXIF rotation,
     * applies safe downsampling to prevent OOM, and writes the normalized
     * high-res JPEG to local app storage.
     */
    suspend fun processAndNormalizeImage(
        context: Context,
        sourceUri: Uri
    ): ProcessedPhotoResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // 1. Read EXIF Orientation
        var rotationDegrees = 0
        var isFlippedHorizontal = false

        try {
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> isFlippedHorizontal = true
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        rotationDegrees = 90
                        isFlippedHorizontal = true
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        rotationDegrees = 270
                        isFlippedHorizontal = true
                    }
                }
            }
        } catch (_: Exception) {
            rotationDegrees = 0
        }

        // 2. Read image bounds safely
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight

        // 3. Compute sample size to avoid OOM while preserving high fidelity
        var sampleSize = 1
        var largestDimension = maxOf(rawWidth, rawHeight)
        while (largestDimension / (sampleSize * 2) >= MAX_DIMENSION) {
            sampleSize *= 2
        }

        // 4. Decode actual Bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val rawBitmap: Bitmap = contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: throw IllegalArgumentException("Could not decode image from URI: $sourceUri")

        // 5. Apply EXIF transformation (Rotation & Flip)
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (isFlippedHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        val correctedBitmap = if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
            if (transformed != rawBitmap) {
                rawBitmap.recycle()
            }
            transformed
        } else {
            rawBitmap
        }

        // 6. Save normalized image to local persistent app storage
        val outputDir = File(context.filesDir, "studio_photos").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val destinationFile = File(outputDir, "photo_${timestamp}.jpg")

        FileOutputStream(destinationFile).use { fos ->
            correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 96, fos)
        }

        val destinationUri = Uri.fromFile(destinationFile)

        ProcessedPhotoResult(
            fileUri = destinationUri,
            filePath = destinationFile.absolutePath,
            bitmap = correctedBitmap,
            originalWidth = rawWidth,
            originalHeight = rawHeight,
            processedWidth = correctedBitmap.width,
            processedHeight = correctedBitmap.height,
            rotationDegrees = rotationDegrees,
            fileSizeBytes = destinationFile.length()
        )
    }

    /**
     * Rotate a bitmap by 90 degrees clockwise safely.
     */
    fun rotateBitmapClockwise(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source && !source.isRecycled) {
            source.recycle()
        }
        return rotated
    }
}
