package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10

data class CropRatioPreset(
    val title: String,
    val ratio: Float,
    val dimensionsLabel: String,
    val isCustom: Boolean = false
) {
    companion object {
        val INDIAN_PASSPORT = CropRatioPreset("Indian Passport", 35f / 45f, "35×45 mm")
        val UPSC_SQUARE = CropRatioPreset("UPSC / SSC Square", 1.0f, "35×35 mm")
        val US_VISA = CropRatioPreset("US Visa / Passport", 1.0f, "2×2 inches")
        val SQUARE_1_1 = CropRatioPreset("1:1 Square", 1.0f, "Square")
        val STAMP_PHOTO = CropRatioPreset("Stamp Photo", 20f / 25f, "20×25 mm")

        val entries: List<CropRatioPreset> = listOf(INDIAN_PASSPORT, UPSC_SQUARE, US_VISA, SQUARE_1_1, STAMP_PHOTO)

        fun fromPhotoSpec(spec: com.gemid.photostudio.data.model.PhotoSpec): CropRatioPreset {
            return CropRatioPreset(
                title = spec.title,
                ratio = spec.aspectRatio,
                dimensionsLabel = spec.dimensionMmLabel,
                isCustom = spec.isCustom
            )
        }
    }
}

@Composable
fun InteractiveCropper(
    bitmap: Bitmap,
    cropPreset: CropRatioPreset,
    modifier: Modifier = Modifier,
    onTransformChange: (scale: Float, panOffset: Offset, cropWindowRect: Rect, canvasSize: Size) -> Unit = { _, _, _, _ -> }
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                    offset = Offset(
                        x = offset.x + pan.x,
                        y = offset.y + pan.y
                    )
                }
            }
    ) {
        val composeBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate Target Crop Window based on preset aspect ratio
            val maxCropW = canvasW * 0.85f
            val maxCropH = canvasH * 0.78f
            val aspect = cropPreset.ratio

            var cropW: Float
            var cropH: Float

            if (maxCropW / aspect <= maxCropH) {
                cropW = maxCropW
                cropH = cropW / aspect
            } else {
                cropH = maxCropH
                cropW = cropH * aspect
            }

            val cropLeft = (canvasW - cropW) / 2f
            val cropTop = (canvasH - cropH) / 2f
            val cropRect = Rect(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)

            // Notify parent about current transformation and crop boundaries
            onTransformChange(scale, offset, cropRect, size)

            // 1. Draw Image with user pan & zoom
            val imgW = bitmap.width.toFloat()
            val imgH = bitmap.height.toFloat()

            // Calculate fit scale so image fills or centers nicely initially
            val baseScale = maxOf(cropW / imgW, cropH / imgH)
            val currentTotalScale = baseScale * scale

            val drawW = imgW * currentTotalScale
            val drawH = imgH * currentTotalScale
            val drawLeft = (canvasW - drawW) / 2f + offset.x
            val drawTop = (canvasH - drawH) / 2f + offset.y

            drawImage(
                image = composeBitmap,
                dstOffset = IntOffset(drawLeft.toInt(), drawTop.toInt()),
                dstSize = IntSize(drawW.toInt(), drawH.toInt())
            )

            // 2. Draw Semi-transparent Scrim outside the crop window
            val cropCornerRadius = 16.dp.toPx()
            val cropPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = cropRect,
                        cornerRadius = CornerRadius(cropCornerRadius, cropCornerRadius)
                    )
                )
            }

            clipPath(cropPath, clipOp = ClipOp.Difference) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.70f),
                    size = size
                )
            }

            // 3. Draw Crop Frame Border & Corner Handles
            drawRoundRect(
                color = AccentCyan,
                topLeft = Offset(cropLeft, cropTop),
                size = Size(cropW, cropH),
                cornerRadius = CornerRadius(cropCornerRadius, cropCornerRadius),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4. Biometric Center Guidelines (Rule of thirds & Eye level)
            val gridStroke = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            val gridColor = Color.White.copy(alpha = 0.25f)

            // Horizontal Thirds
            drawLine(
                color = gridColor,
                start = Offset(cropLeft, cropTop + cropH / 3f),
                end = Offset(cropLeft + cropW, cropTop + cropH / 3f),
                strokeWidth = gridStroke.width,
                pathEffect = gridStroke.pathEffect
            )
            drawLine(
                color = gridColor,
                start = Offset(cropLeft, cropTop + (cropH * 2f / 3f)),
                end = Offset(cropLeft + cropW, cropTop + (cropH * 2f / 3f)),
                strokeWidth = gridStroke.width,
                pathEffect = gridStroke.pathEffect
            )

            // Vertical Center
            val centerX = cropLeft + cropW / 2f
            drawLine(
                color = AccentTeal.copy(alpha = 0.4f),
                start = Offset(centerX, cropTop),
                end = Offset(centerX, cropTop + cropH),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // Subtle Eye-level guideline at ~38% from top
            val eyeLevel = cropTop + (cropH * 0.38f)
            drawLine(
                color = AccentTeal.copy(alpha = 0.8f),
                start = Offset(centerX - cropW * 0.25f, eyeLevel),
                end = Offset(centerX + cropW * 0.25f, eyeLevel),
                strokeWidth = 2.dp.toPx()
            )

            // Biometric Face Oval Outline (ICAO 70-80% height guideline)
            val ovalW = cropW * 0.52f
            val ovalH = cropH * 0.62f
            drawOval(
                color = AccentCyan.copy(alpha = 0.5f),
                topLeft = Offset(centerX - ovalW / 2f, cropTop + cropH * 0.12f),
                size = Size(ovalW, ovalH),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )
        }

        // Top-center indicator chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Navy10.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "${cropPreset.title} (${cropPreset.dimensionsLabel}) • Pinch to Zoom / Drag to Align",
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

object CropCalculator {
    /**
     * Extracts the cropped sub-bitmap corresponding to the screen crop window and transformations.
     */
    fun performCrop(
        source: Bitmap,
        cropRect: Rect,
        canvasSize: Size,
        scale: Float,
        panOffset: Offset
    ): Bitmap {
        val imgW = source.width.toFloat()
        val imgH = source.height.toFloat()

        val baseScale = maxOf(cropRect.width / imgW, cropRect.height / imgH)
        val currentTotalScale = baseScale * scale

        val drawW = imgW * currentTotalScale
        val drawH = imgH * currentTotalScale
        val drawLeft = (canvasSize.width - drawW) / 2f + panOffset.x
        val drawTop = (canvasSize.height - drawH) / 2f + panOffset.y

        // Calculate source bitmap crop rectangle
        val srcLeft = ((cropRect.left - drawLeft) / currentTotalScale).coerceIn(0f, imgW)
        val srcTop = ((cropRect.top - drawTop) / currentTotalScale).coerceIn(0f, imgH)
        val srcRight = ((cropRect.right - drawLeft) / currentTotalScale).coerceIn(0f, imgW)
        val srcBottom = ((cropRect.bottom - drawTop) / currentTotalScale).coerceIn(0f, imgH)

        val cropWidth = (srcRight - srcLeft).toInt().coerceAtLeast(1)
        val cropHeight = (srcBottom - srcTop).toInt().coerceAtLeast(1)

        val x = srcLeft.toInt().coerceIn(0, source.width - cropWidth)
        val y = srcTop.toInt().coerceIn(0, source.height - cropHeight)

        return Bitmap.createBitmap(source, x, y, cropWidth, cropHeight)
    }
}
