package com.gemid.photostudio.batch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.editor.BackgroundEngine
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.editor.awaitTask
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Automated Photo Optimization Engine for 1-Tap Batch Studio.
 * Executes:
 * 1. Auto-Face Detection & Biometric Cropping (ICAO 70-80% proportions)
 * 2. Studio Background Cleanup (Pure White #FFFFFF or Sky Blue #0077BE)
 * 3. Studio-Grade Auto-Enhancement (Skin tone balancing & shadow lifting)
 */
object AutoPassportOptimizer {

    suspend fun optimizePassportPhoto(
        rawBitmap: Bitmap,
        template: PassportTemplate = PassportTemplate.INDIAN_PASSPORT,
        targetBackdrop: StudioBackdrop = StudioBackdrop.WHITE,
        autoCrop: Boolean = true,
        autoCleanBackground: Boolean = true,
        autoEnhanceTone: Boolean = true,
        onStepUpdate: (String) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        var currentBitmap = rawBitmap

        // ── 1. Auto-Face Detection & Biometric Cropping ───────────────
        if (autoCrop) {
            onStepUpdate("Detecting face & biometric cropping...")
            currentBitmap = detectFaceAndCrop(currentBitmap, template)
        }

        // ── 2. Studio Background Cleanup ──────────────────────────────
        if (autoCleanBackground && targetBackdrop != StudioBackdrop.ORIGINAL) {
            onStepUpdate("Applying ${targetBackdrop.title} studio backdrop...")
            currentBitmap = BackgroundEngine.replaceBackground(currentBitmap, targetBackdrop)
        }

        // ── 3. Auto-Enhancement (Tone, Contrast & Exposure) ───────────
        if (autoEnhanceTone) {
            onStepUpdate("Balancing skin tones & studio lighting...")
            currentBitmap = applyStudioAutoEnhancement(currentBitmap)
        }

        onStepUpdate("Completed")
        return@withContext currentBitmap
    }

    /**
     * Detects facial landmarks and centers head & shoulders matching passport standards.
     */
    private suspend fun detectFaceAndCrop(
        source: Bitmap,
        template: PassportTemplate
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.12f)
                .build()

            val detector = FaceDetection.getClient(options)
            val inputImage = InputImage.fromBitmap(source, 0)
            val faces: List<Face> = detector.process(inputImage).awaitTask()

            if (faces.isNotEmpty()) {
                // Find primary / largest face in the photo
                val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                val box = face.boundingBox

                val faceCenterX = box.centerX().toFloat()
                val faceCenterY = box.centerY().toFloat()
                val faceH = box.height().toFloat()

                // Biometric passport standard: full head (chin to hair crown) is ~1.35x face box height
                val estimatedHeadHeight = faceH * 1.35f

                // In standard passport guidelines, head occupies ~74% of the photo height
                val targetCropH = estimatedHeadHeight / 0.74f
                val targetAspect = template.aspectRatio // e.g. 35/45 = 0.777f for Indian Passport
                val targetCropW = targetCropH * targetAspect

                // Ensure crop rectangle fits within original image dimensions
                val maxAllowedW = source.width.toFloat()
                val maxAllowedH = source.height.toFloat()

                var finalCropW = targetCropW
                var finalCropH = targetCropH

                if (finalCropW > maxAllowedW || finalCropH > maxAllowedH) {
                    val scaleFactor = minOf(maxAllowedW / finalCropW, maxAllowedH / finalCropH)
                    finalCropW *= scaleFactor
                    finalCropH *= scaleFactor
                }

                // Center horizontally on face center
                var left = faceCenterX - (finalCropW / 2f)
                var right = left + finalCropW

                // Adjust horizontal bounds if overflowing
                if (left < 0f) {
                    right += -left
                    left = 0f
                }
                if (right > maxAllowedW) {
                    left -= (right - maxAllowedW)
                    right = maxAllowedW
                }
                left = left.coerceIn(0f, maxAllowedW)
                right = right.coerceIn(0f, maxAllowedW)

                // Align top so hair crown has ~10% margin above it
                val headTopEstimate = box.top - (faceH * 0.35f)
                var top = headTopEstimate - (finalCropH * 0.10f)
                var bottom = top + finalCropH

                // Adjust vertical bounds if overflowing
                if (top < 0f) {
                    bottom += -top
                    top = 0f
                }
                if (bottom > maxAllowedH) {
                    top -= (bottom - maxAllowedH)
                    bottom = maxAllowedH
                }
                top = top.coerceIn(0f, maxAllowedH)
                bottom = bottom.coerceIn(0f, maxAllowedH)

                val cropRect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

                if (cropRect.width() > 10 && cropRect.height() > 10) {
                    val cropped = Bitmap.createBitmap(
                        source,
                        cropRect.left,
                        cropRect.top,
                        cropRect.width(),
                        cropRect.height()
                    )

                    // Scale to standard 300 DPI target resolution
                    val standardDpi = 300
                    val targetW = (template.widthMm / 25.4f * standardDpi).toInt()
                    val targetH = (template.heightMm / 25.4f * standardDpi).toInt()

                    return@withContext Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
                }
            }
        } catch (_: Throwable) {
            // If ML Kit fails or is unavailable, smoothly fallback to center crop
        }

        // Fallback: Smart centered portrait crop
        return@withContext fallbackCenterCrop(source, template)
    }

    /**
     * Fallback centered portrait crop when facial landmarks are not detected.
     */
    private fun fallbackCenterCrop(source: Bitmap, template: PassportTemplate): Bitmap {
        val targetAspect = template.aspectRatio
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val srcAspect = srcW / srcH

        val cropW: Float
        val cropH: Float

        if (srcAspect > targetAspect) {
            cropH = srcH
            cropW = srcH * targetAspect
        } else {
            cropW = srcW
            cropH = srcW / targetAspect
        }

        val left = maxOf(0f, (srcW - cropW) / 2f)
        val top = maxOf(0f, (srcH - cropH) * 0.15f) // slightly favor top for head and shoulders
        val right = minOf(srcW, left + cropW)
        val bottom = minOf(srcH, top + cropH)

        val cropped = Bitmap.createBitmap(
            source,
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (bottom - top).toInt()
        )

        val targetW = (template.widthMm / 25.4f * 300f).toInt()
        val targetH = (template.heightMm / 25.4f * 300f).toInt()

        return Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
    }

    /**
     * Studio-grade automatic tone and exposure enhancement.
     * Evaluates average portrait luminance, lifts underexposed shadows,
     * boosts feature contrast, and balances skin tone vitality.
     */
    private fun applyStudioAutoEnhancement(source: Bitmap): Bitmap {
        // 1. Sample luminance across facial/central portrait area
        var totalLum = 0L
        var sampleCount = 0
        val step = maxOf(1, source.width / 40)

        val startX = (source.width * 0.2f).toInt()
        val endX = (source.width * 0.8f).toInt()
        val startY = (source.height * 0.15f).toInt()
        val endY = (source.height * 0.75f).toInt()

        for (y in startY until endY step step) {
            for (x in startX until endX step step) {
                val pixel = source.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toLong()
                totalLum += lum
                sampleCount++
            }
        }

        val avgLuminance = if (sampleCount > 0) (totalLum / sampleCount).toInt() else 130

        // 2. Compute dynamic adjustment parameters
        val brightnessShift: Float
        val contrastFactor: Float
        val saturationFactor: Float
        val warmthShift: Float

        when {
            avgLuminance < 115 -> {
                // Underexposed portrait: lift shadows significantly
                brightnessShift = 24f
                contrastFactor = 1.15f
                saturationFactor = 1.08f
                warmthShift = 4f
            }
            avgLuminance in 115..160 -> {
                // Well-balanced ambient light: polish clarity and vibrancy
                brightnessShift = 12f
                contrastFactor = 1.12f
                saturationFactor = 1.06f
                warmthShift = 3f
            }
            else -> {
                // High key / bright portrait: protect highlights
                brightnessShift = -4f
                contrastFactor = 1.08f
                saturationFactor = 1.04f
                warmthShift = 1f
            }
        }

        // 3. Build native Android ColorMatrix
        val matrix = AndroidColorMatrix()

        // Brightness & Exposure Matrix
        val bMatrix = AndroidColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessShift,
                0f, 1f, 0f, 0f, brightnessShift,
                0f, 0f, 1f, 0f, brightnessShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(bMatrix)

        // Contrast Matrix
        val translate = (-0.5f * contrastFactor + 0.5f) * 255f
        val cMatrix = AndroidColorMatrix(
            floatArrayOf(
                contrastFactor, 0f, 0f, 0f, translate,
                0f, contrastFactor, 0f, 0f, translate,
                0f, 0f, contrastFactor, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(cMatrix)

        // Saturation Matrix
        val sMatrix = AndroidColorMatrix().apply {
            setSaturation(saturationFactor)
        }
        matrix.postConcat(sMatrix)

        // Skin Warmth Matrix (gentle red lift & blue balance)
        val wMatrix = AndroidColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, warmthShift * 0.8f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -warmthShift * 0.8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(wMatrix)

        // 4. Paint onto resulting bitmap
        val enhancedBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        return enhancedBitmap
    }
}
