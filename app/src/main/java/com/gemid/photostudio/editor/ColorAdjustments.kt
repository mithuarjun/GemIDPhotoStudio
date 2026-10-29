package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

/**
 * Photoshop-style color and tone adjustment parameters.
 * Exposure: -1.0 to +1.0 (default 0)
 * Brightness: -100 to +100 (default 0)
 * Contrast: 0.5 to 2.0 (default 1.0)
 * Saturation: 0.0 to 2.0 (default 1.0)
 * Warmth (Color Temperature): -50 to +50 (default 0)
 */
data class ToneAdjustments(
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f
) {
    val isDefault: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 1f && saturation == 1f && warmth == 0f

    /**
     * Builds the combined native Android ColorMatrix.
     */
    fun toAndroidColorMatrix(): AndroidColorMatrix {
        val matrix = AndroidColorMatrix()

        // 1. Exposure & Brightness combined
        val totalBrightness = (brightness + (exposure * 60f))
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

        // 4. Color Temperature (Warmth)
        // Warm shifts Red positive and Blue negative; Cool shifts Blue positive and Red negative
        val warmthShift = warmth * 0.8f
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

    /**
     * Builds a Compose ColorMatrix for instant 60fps GPU preview.
     */
    fun toComposeColorMatrix(): ComposeColorMatrix {
        val androidMatrix = toAndroidColorMatrix()
        return ComposeColorMatrix(androidMatrix.array)
    }

    /**
     * Bakes the color adjustments directly onto a new native Bitmap.
     */
    fun applyToBitmap(source: Bitmap): Bitmap {
        if (isDefault) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(toAndroidColorMatrix())
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
