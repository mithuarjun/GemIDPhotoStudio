package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

/**
 * Professional Studio Color & Tone Adjustment Suite.
 * - Brightness: -50 to +50 (default 0)
 * - Contrast: 0.6 to 1.6 (default 1.0)
 * - Warmth: -40 to +40 (default 0, skin tone balancing)
 * - Sharpness: 0.0 to 1.0 (default 0, edge enhancement)
 * - Skin Smooth / Beautify: 0.0 to 1.0 (default 0, soft local skin finishing)
 */
data class ToneAdjustments(
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val sharpness: Float = 0f,
    val skinSmooth: Float = 0f
) {
    val isDefault: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 1f && saturation == 1f &&
                warmth == 0f && sharpness == 0f && skinSmooth == 0f

    /**
     * Builds the combined native Android ColorMatrix for brightness, contrast, saturation, and warmth.
     */
    fun toAndroidColorMatrix(): AndroidColorMatrix {
        val matrix = AndroidColorMatrix()

        // 1. Brightness & Exposure combined
        val totalBrightness = (brightness + (exposure * 50f))
        val brightnessMatrix = AndroidColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, totalBrightness,
                0f, 1f, 0f, 0f, totalBrightness,
                0f, 0f, 1f, 0f, totalBrightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(brightnessMatrix)

        // 2. Contrast
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = AndroidColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(contrastMatrix)

        // 3. Saturation
        val satMatrix = AndroidColorMatrix().apply {
            setSaturation(saturation)
        }
        matrix.postConcat(satMatrix)

        // 4. Color Warmth (Skin tone temperature balance)
        // Warm shifts Red positive and Blue negative; Cool shifts Blue positive and Red negative
        val warmthShift = warmth * 0.75f
        val warmthMatrix = AndroidColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, warmthShift,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -warmthShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(warmthMatrix)

        return matrix
    }

    fun toComposeColorMatrix(): ComposeColorMatrix {
        val androidMatrix = toAndroidColorMatrix()
        return ComposeColorMatrix(androidMatrix.array)
    }

    /**
     * Applies tone adjustments, skin smoothing beautification, and sharpness directly onto bitmap.
     */
    fun applyToBitmap(source: Bitmap): Bitmap {
        if (isDefault) return source

        val width = source.width
        val height = source.height

        // 1. Apply Color Matrix (Brightness, Contrast, Saturation, Warmth)
        val colorTuned = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(colorTuned)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(toAndroidColorMatrix())
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // If no skin smoothing or sharpness requested, return color tuned bitmap directly
        if (skinSmooth <= 0.01f && sharpness <= 0.01f) {
            return colorTuned
        }

        // 2. Local Bilateral / Surface Blending for Skin Smoothing & Sharpness
        val srcPixels = IntArray(width * height)
        colorTuned.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        // Create a fast smoothed approximation for bilateral blending
        val scaleDownFactor = 3
        val smallW = maxOf(10, width / scaleDownFactor)
        val smallH = maxOf(10, height / scaleDownFactor)
        val smallBitmap = Bitmap.createScaledBitmap(colorTuned, smallW, smallH, true)
        val blurredBitmap = Bitmap.createScaledBitmap(smallBitmap, width, height, true)
        smallBitmap.recycle()

        val blurPixels = IntArray(width * height)
        blurredBitmap.getPixels(blurPixels, 0, width, 0, 0, width, height)
        blurredBitmap.recycle()

        val smoothWeight = skinSmooth.coerceIn(0f, 1f) * 0.70f
        val sharpWeight = sharpness.coerceIn(0f, 1f) * 0.85f

        for (i in 0 until (width * height)) {
            val srcP = srcPixels[i]
            val blurP = blurPixels[i]

            val sa = (srcP ushr 24) and 0xFF
            val sr = (srcP ushr 16) and 0xFF
            val sg = (srcP ushr 8) and 0xFF
            val sb = srcP and 0xFF

            val br = (blurP ushr 16) and 0xFF
            val bg = (blurP ushr 8) and 0xFF
            val bb = blurP and 0xFF

            // Edge threshold check (Preserves high-contrast edges like eyes, hair, and outlines)
            val diff = kotlin.math.abs(sr - br) + kotlin.math.abs(sg - bg) + kotlin.math.abs(sb - bb)

            var finalR = sr.toFloat()
            var finalG = sg.toFloat()
            var finalB = sb.toFloat()

            // Skin Smoothing: Smooth only low-to-medium contrast areas (pores, minor blemishes)
            if (smoothWeight > 0f && diff < 85) {
                val blendFactor = smoothWeight * (1f - (diff / 85f))
                finalR = (1f - blendFactor) * finalR + blendFactor * br
                finalG = (1f - blendFactor) * finalG + blendFactor * bg
                finalB = (1f - blendFactor) * finalB + blendFactor * bb
            }

            // Sharpness enhancement: boost high-frequency details (eyes, eyebrows, suits, borders)
            if (sharpWeight > 0f) {
                finalR += sharpWeight * (sr - br)
                finalG += sharpWeight * (sg - bg)
                finalB += sharpWeight * (sb - bb)
            }

            val cr = finalR.toInt().coerceIn(0, 255)
            val cg = finalG.toInt().coerceIn(0, 255)
            val cb = finalB.toInt().coerceIn(0, 255)

            outPixels[i] = (sa shl 24) or (cr shl 16) or (cg shl 8) or cb
        }

        colorTuned.recycle()

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
