package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class StudioBackdrop(
    val title: String,
    val colorArgb: Int,
    val hexString: String
) {
    ORIGINAL("Original", 0, "Default"),
    WHITE("White", 0xFFFFFFFF.toInt(), "#FFFFFF"),
    SKY_BLUE("Sky Blue", 0xFF0077BE.toInt(), "#0077BE"),
    LIGHT_BLUE("Passport Blue", 0xFF38BDF8.toInt(), "#38BDF8"),
    LIGHT_GRAY("Neutral Gray", 0xFFE5E7EB.toInt(), "#E5E7EB"),
    TRANSPARENT("Transparent", 0x00000000, "#00000000")
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    addOnCanceledListener { continuation.cancel() }
}

object BackgroundEngine {

    /**
     * Replaces the background of [sourceBitmap] with [targetBackdrop].
     * Uses Google ML Kit Selfie Segmentation with an automatic fallback to
     * fast edge-aware color segmentation if ML Kit is unavailable.
     */
    suspend fun replaceBackground(
        sourceBitmap: Bitmap,
        targetBackdrop: StudioBackdrop
    ): Bitmap = withContext(Dispatchers.Default) {
        if (targetBackdrop == StudioBackdrop.ORIGINAL) {
            return@withContext sourceBitmap
        }

        val mask = generateForegroundMask(sourceBitmap)
        return@withContext applyMaskToBitmap(sourceBitmap, mask, targetBackdrop.colorArgb)
    }

    /**
     * Extracts an 8-bit foreground alpha mask (0 = background, 255 = foreground)
     * using Google ML Kit Selfie Segmentation with automatic color-distance fallback.
     */
    suspend fun generateForegroundMask(sourceBitmap: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val maskArray = ByteArray(width * height)

        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
                .build()

            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(sourceBitmap, 0)
            val mask = segmenter.process(inputImage).awaitTask()

            val maskBuffer: ByteBuffer = mask.buffer
            maskBuffer.rewind()

            for (i in 0 until (width * height)) {
                val confidence = maskBuffer.float // 0.0f = background, 1.0f = person foreground
                val byteVal = when {
                    confidence >= 0.75f -> 255
                    confidence <= 0.25f -> 0
                    else -> (((confidence - 0.25f) / 0.50f) * 255f).toInt().coerceIn(0, 255)
                }
                maskArray[i] = byteVal.toByte()
            }

            segmenter.close()
            return@withContext maskArray
        } catch (_: Throwable) {
            return@withContext runColorMaskFallback(sourceBitmap)
        }
    }

    /**
     * Real-time zero-latency bit-shifting composite generator.
     * Blends source pixels with backdrop color according to the 8-bit mask.
     */
    fun applyMaskToBitmap(
        sourceBitmap: Bitmap,
        mask: ByteArray,
        backdropColor: Int
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val srcPixels = IntArray(width * height)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val dstPixels = IntArray(width * height)

        val repA = (backdropColor shr 24) and 0xFF
        val repR = (backdropColor shr 16) and 0xFF
        val repG = (backdropColor shr 8) and 0xFF
        val repB = backdropColor and 0xFF

        for (i in 0 until (width * height)) {
            val maskVal = mask[i].toInt() and 0xFF
            if (maskVal == 255) {
                dstPixels[i] = srcPixels[i]
            } else if (maskVal == 0) {
                dstPixels[i] = backdropColor
            } else {
                val srcPixel = srcPixels[i]
                val srcA = (srcPixel shr 24) and 0xFF
                val srcR = (srcPixel shr 16) and 0xFF
                val srcG = (srcPixel shr 8) and 0xFF
                val srcB = srcPixel and 0xFF

                val invMask = 255 - maskVal
                val outA = (srcA * maskVal + repA * invMask) / 255
                val outR = (srcR * maskVal + repR * invMask) / 255
                val outG = (srcG * maskVal + repG * invMask) / 255
                val outB = (srcB * maskVal + repB * invMask) / 255

                dstPixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Generates a high-contrast Black & White visual representation of the alpha mask
     * (White = foreground kept, Black = background removed).
     */
    fun createAlphaMaskBitmap(
        mask: ByteArray,
        width: Int,
        height: Int
    ): Bitmap {
        val maskPixels = IntArray(width * height)
        for (i in 0 until (width * height)) {
            val v = mask[i].toInt() and 0xFF
            maskPixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(maskPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Generates a Photoshop-style "Ruby Mask" overlay where background areas are
     * tinted with translucent studio ruby red (#FF1744) for easy edge inspection.
     */
    fun createRubyMaskBitmap(
        sourceBitmap: Bitmap,
        mask: ByteArray
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val srcPixels = IntArray(width * height)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val dstPixels = IntArray(width * height)

        for (i in 0 until (width * height)) {
            val maskVal = mask[i].toInt() and 0xFF
            val srcPixel = srcPixels[i]
            if (maskVal == 255) {
                dstPixels[i] = srcPixel
            } else {
                val sr = (srcPixel shr 16) and 0xFF
                val sg = (srcPixel shr 8) and 0xFF
                val sb = srcPixel and 0xFF
                val rubyFactor = ((255 - maskVal) / 255f) * 0.65f
                val r = (sr * (1f - rubyFactor) + 255f * rubyFactor).toInt().coerceIn(0, 255)
                val g = (sg * (1f - rubyFactor)).toInt().coerceIn(0, 255)
                val b = (sb * (1f - rubyFactor) + 40f * rubyFactor).toInt().coerceIn(0, 255)
                dstPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Fast, lightweight color similarity fallback that samples corners
     * and perimeter to remove monochrome/wall backdrops when ML Kit is unavailable.
     */
    private fun runColorMaskFallback(sourceBitmap: Bitmap): ByteArray {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val maskArray = ByteArray(width * height)
        val srcPixels = IntArray(width * height)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // Sample background color from top corners
        val corner1 = srcPixels[0]
        val corner2 = srcPixels[width - 1]
        val corner3 = srcPixels[minOf(10 * width, srcPixels.size - 1)]

        val bgR = (((corner1 shr 16) and 0xFF) + ((corner2 shr 16) and 0xFF) + ((corner3 shr 16) and 0xFF)) / 3
        val bgG = (((corner1 shr 8) and 0xFF) + ((corner2 shr 8) and 0xFF) + ((corner3 shr 8) and 0xFF)) / 3
        val bgB = ((corner1 and 0xFF) + (corner2 and 0xFF) + (corner3 and 0xFF)) / 3

        val threshold = 48.0

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val index = rowOffset + x
                val pixel = srcPixels[index]

                val pr = (pixel shr 16) and 0xFF
                val pg = (pixel shr 8) and 0xFF
                val pb = pixel and 0xFF

                val dist = Math.sqrt(
                    ((pr - bgR) * (pr - bgR) + (pg - bgG) * (pg - bgG) + (pb - bgB) * (pb - bgB)).toDouble()
                )

                val isCenterZone = x > (width * 0.25f) && x < (width * 0.75f) && y > (height * 0.20f) && y < (height * 0.70f)
                val maskVal = when {
                    isCenterZone -> 255
                    dist < threshold -> 0
                    dist < threshold + 15.0 -> (((dist - threshold) / 15.0) * 255f).toInt().coerceIn(0, 255)
                    else -> 255
                }
                maskArray[index] = maskVal.toByte()
            }
        }
        return maskArray
    }
}
