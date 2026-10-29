package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Rect
import java.util.Stack
import kotlin.math.hypot

enum class RefineBrushMode {
    ERASE,   // Removes areas from the foreground mask (clears to background)
    RESTORE  // Recovers clipped hair/shoulders (brings original pixels back into foreground)
}

/**
 * High-performance, 100% offline real-time bit-shifting pixel refiner.
 * Manages mutable mask byte array and synchronizes native ARGB composite and
 * Black & White alpha mask bitmaps with zero latency.
 */
class MaskRefinerEngine(
    val sourceBitmap: Bitmap,
    initialMask: ByteArray,
    var backdropColor: Int = 0xFFFFFFFF.toInt() // Default Studio White
) {
    val width: Int = sourceBitmap.width
    val height: Int = sourceBitmap.height

    // 8-bit Alpha Mask (0 = background, 255 = foreground)
    val mask: ByteArray = initialMask.clone()

    // Pre-allocated native pixel arrays for ultra-fast bit-shifting updates
    private val srcPixels = IntArray(width * height)
    private val compositePixels = IntArray(width * height)
    private val maskDisplayPixels = IntArray(width * height)

    // Live display bitmaps bound to Compose Canvas
    val compositeBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val maskBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Bounded Undo / Redo history stacks (max 6 snapshots)
    private val undoStack = Stack<ByteArray>()
    private val redoStack = Stack<ByteArray>()
    private val MAX_HISTORY = 6

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    init {
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        fullRecompute()
    }

    /**
     * Recomputes all pixels across both composite and mask bitmaps using bit-shifting.
     */
    fun fullRecompute() {
        val repA = (backdropColor shr 24) and 0xFF
        val repR = (backdropColor shr 16) and 0xFF
        val repG = (backdropColor shr 8) and 0xFF
        val repB = backdropColor and 0xFF

        for (i in 0 until (width * height)) {
            val maskVal = mask[i].toInt() and 0xFF

            // 1. Black & White Alpha Mask visualization: (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            maskDisplayPixels[i] = (0xFF shl 24) or (maskVal shl 16) or (maskVal shl 8) or maskVal

            // 2. High-speed bit-shifting alpha blend
            if (maskVal == 255) {
                compositePixels[i] = srcPixels[i]
            } else if (maskVal == 0) {
                compositePixels[i] = backdropColor
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

                compositePixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        compositeBitmap.setPixels(compositePixels, 0, width, 0, 0, width, height)
        maskBitmap.setPixels(maskDisplayPixels, 0, width, 0, 0, width, height)
    }

    /**
     * Changes the preview studio backdrop color and updates composite pixels immediately.
     */
    fun updateBackdropColor(newColor: Int) {
        if (backdropColor != newColor) {
            backdropColor = newColor
            fullRecompute()
        }
    }

    /**
     * Records a historical snapshot before a new stroke begins.
     */
    fun beginStroke() {
        if (undoStack.size >= MAX_HISTORY) {
            undoStack.removeAt(0)
        }
        undoStack.push(mask.clone())
        redoStack.clear()
    }

    /**
     * Applies continuous touch stroke with sub-pixel interpolation,
     * updates affected bounding box, and performs fast bit-shifting updates with zero latency.
     */
    fun applyStroke(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        radius: Float,
        mode: RefineBrushMode,
        hardness: Float = 0.85f
    ): Rect {
        val dist = hypot(toX - fromX, toY - fromY)
        val stepSize = maxOf(1f, radius * 0.25f)
        val steps = maxOf(1, (dist / stepSize).toInt())

        var overallMinX = width
        var overallMaxX = 0
        var overallMinY = height
        var overallMaxY = 0

        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val curX = fromX + (toX - fromX) * t
            val curY = fromY + (toY - fromY) * t

            val dirty = stampCircle(curX, curY, radius, mode, hardness)
            if (!dirty.isEmpty) {
                overallMinX = minOf(overallMinX, dirty.left)
                overallMaxX = maxOf(overallMaxX, dirty.right)
                overallMinY = minOf(overallMinY, dirty.top)
                overallMaxY = maxOf(overallMaxY, dirty.bottom)
            }
        }

        if (overallMinX <= overallMaxX && overallMinY <= overallMaxY) {
            val dirtyRect = Rect(overallMinX, overallMinY, overallMaxX + 1, overallMaxY + 1)
            flushDirtyRect(dirtyRect)
            return dirtyRect
        }

        return Rect(0, 0, 0, 0)
    }

    /**
     * Stamps a single circular footprint into the mask byte array.
     */
    private fun stampCircle(
        centerX: Float,
        centerY: Float,
        radius: Float,
        mode: RefineBrushMode,
        hardness: Float
    ): Rect {
        val r = radius.toInt()
        val minX = maxOf(0, (centerX - r).toInt())
        val maxX = minOf(width - 1, (centerX + r).toInt())
        val minY = maxOf(0, (centerY - r).toInt())
        val maxY = minOf(height - 1, (centerY + r).toInt())

        if (minX > maxX || minY > maxY) return Rect(0, 0, 0, 0)

        val rSq = radius * radius
        val innerRadius = radius * hardness.coerceIn(0.1f, 1.0f)
        val innerRadiusSq = innerRadius * innerRadius
        val featherRange = maxOf(1f, radius - innerRadius)

        val targetVal = if (mode == RefineBrushMode.ERASE) 0 else 255

        val repA = (backdropColor shr 24) and 0xFF
        val repR = (backdropColor shr 16) and 0xFF
        val repG = (backdropColor shr 8) and 0xFF
        val repB = backdropColor and 0xFF

        for (y in minY..maxY) {
            val dy = y - centerY
            val dySq = dy * dy
            val rowOffset = y * width

            for (x in minX..maxX) {
                val dx = x - centerX
                val distSq = dx * dx + dySq

                if (distSq <= rSq) {
                    val idx = rowOffset + x
                    val currentVal = mask[idx].toInt() and 0xFF

                    val factor = if (distSq <= innerRadiusSq) {
                        1.0f
                    } else {
                        val dist = Math.sqrt(distSq.toDouble()).toFloat()
                        ((radius - dist) / featherRange).coerceIn(0f, 1f)
                    }

                    val newVal = (currentVal + (targetVal - currentVal) * factor).toInt().coerceIn(0, 255)
                    mask[idx] = newVal.toByte()

                    // Real-time bit-shifting update to cached pixel arrays
                    maskDisplayPixels[idx] = (0xFF shl 24) or (newVal shl 16) or (newVal shl 8) or newVal

                    if (newVal == 255) {
                        compositePixels[idx] = srcPixels[idx]
                    } else if (newVal == 0) {
                        compositePixels[idx] = backdropColor
                    } else {
                        val srcPixel = srcPixels[idx]
                        val srcA = (srcPixel shr 24) and 0xFF
                        val srcR = (srcPixel shr 16) and 0xFF
                        val srcG = (srcPixel shr 8) and 0xFF
                        val srcB = srcPixel and 0xFF

                        val invMask = 255 - newVal
                        val outA = (srcA * newVal + repA * invMask) / 255
                        val outR = (srcR * newVal + repR * invMask) / 255
                        val outG = (srcG * newVal + repG * invMask) / 255
                        val outB = (srcB * newVal + repB * invMask) / 255

                        compositePixels[idx] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
                    }
                }
            }
        }

        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Flushes only the dirty rectangle to the on-screen bitmaps.
     */
    private fun flushDirtyRect(dirty: Rect) {
        val rectW = dirty.width()
        val rectH = dirty.height()
        if (rectW <= 0 || rectH <= 0) return

        val offset = dirty.top * width + dirty.left

        compositeBitmap.setPixels(
            compositePixels,
            offset,
            width,
            dirty.left,
            dirty.top,
            rectW,
            rectH
        )

        maskBitmap.setPixels(
            maskDisplayPixels,
            offset,
            width,
            dirty.left,
            dirty.top,
            rectW,
            rectH
        )
    }

    /**
     * Reverts to the previous mask snapshot.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.push(mask.clone())
        val previous = undoStack.pop()
        System.arraycopy(previous, 0, mask, 0, mask.size)
        fullRecompute()
        return true
    }

    /**
     * Re-applies the next mask snapshot.
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.push(mask.clone())
        val next = redoStack.pop()
        System.arraycopy(next, 0, mask, 0, mask.size)
        fullRecompute()
        return true
    }

    /**
     * Resets mask back to initial AI segmentation.
     */
    fun resetMask(initialMask: ByteArray) {
        beginStroke()
        System.arraycopy(initialMask, 0, mask, 0, mask.size)
        fullRecompute()
    }
}
