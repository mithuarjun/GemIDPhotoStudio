package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.sheet.GridMatrixCalculator
import com.gemid.photostudio.sheet.GridMatrixResult
import com.gemid.photostudio.sheet.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance 300 DPI Sheet Compositing and Image Processing Engine.
 * Supports mixed photo queues, custom borders, dashed guidelines, and physical scissor marks.
 */
object ImageProcessor {

    /**
     * Generates a 300 DPI composite print sheet bitmap.
     *
     * @param photos List of photo bitmaps to place onto the sheet. Automatically loops
     *               through this queue to fill each grid cell sequentially.
     * @param paperSize Standard paper size definition (4x6, 5x7, A4, Letter).
     * @param template Passport specification providing exact millimeter dimensions.
     * @param isLandscape Whether the print sheet is oriented horizontally.
     * @param showBorders Whether to draw borders around individual passport photos.
     * @param borderColor Color of the photo borders (defaults to android.graphics.Color.LTGRAY).
     * @param borderWidth Stroke width of photo borders in pixels.
     * @param showScissorMarks Whether to render dashed cutting guidelines and "✂" scissor symbols.
     * @param customPhotoCount Optional override for number of photo slots to populate.
     * @param includeStudioWatermark Whether to print subtle studio metadata in the sheet margin.
     */
    suspend fun createMixedPrintSheet(
        photos: List<Bitmap>,
        paperSize: PaperSize = PaperSize.POSTCARD_4X6,
        template: PhotoSpec = PassportTemplate.INDIAN_PASSPORT,
        isLandscape: Boolean = false,
        showBorders: Boolean = true,
        borderColor: Int = Color.LTGRAY,
        borderWidth: Float = 3f,
        showScissorMarks: Boolean = true,
        customPhotoCount: Int? = null,
        includeStudioWatermark: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        val grid: GridMatrixResult = GridMatrixCalculator.computeGrid(
            paper = paperSize.definition,
            photoWidthMm = template.widthMm,
            photoHeightMm = template.heightMm,
            isLandscape = isLandscape,
            customCount = customPhotoCount
        )

        val sheetBitmap = Bitmap.createBitmap(
            grid.sheetWidthPx,
            grid.sheetHeightPx,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(sheetBitmap)

        // 1. Fill Sheet with Pure Print White (industry standard photo paper base)
        canvas.drawColor(Color.WHITE)

        val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        // 2. Photo Border Paint
        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = borderWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // 3. Draw Photos into each Grid Cell with Zero-Wastage Aspect-Ratio Preserving Center-Crop
        if (photos.isNotEmpty()) {
            val numPhotos = photos.size
            val totalSlots = grid.cells.size
            val slotsPerPhoto = maxOf(1, totalSlots / numPhotos)

            for (cell in grid.cells) {
                // Smart Multi-Image Queue: Divides slots evenly
                val photoIndex = minOf(cell.index / slotsPerPhoto, numPhotos - 1)
                val photoToDraw = photos[photoIndex]

                val cellW = cell.bounds.width()
                val cellH = cell.bounds.height()
                val cellAspect = if (cellH > 0f) cellW / cellH else 1.0f
                val photoAspect = if (photoToDraw.height > 0) photoToDraw.width.toFloat() / photoToDraw.height.toFloat() else 1.0f

                val srcRect = if (photoAspect > cellAspect) {
                    val targetW = (photoToDraw.height * cellAspect).toInt().coerceAtMost(photoToDraw.width)
                    val offsetX = ((photoToDraw.width - targetW) / 2).coerceAtLeast(0)
                    Rect(offsetX, 0, offsetX + targetW, photoToDraw.height)
                } else {
                    val targetH = (photoToDraw.width / cellAspect).toInt().coerceAtMost(photoToDraw.height)
                    val offsetY = ((photoToDraw.height - targetH) / 2).coerceAtLeast(0)
                    Rect(0, offsetY, photoToDraw.width, offsetY + targetH)
                }

                canvas.drawBitmap(photoToDraw, srcRect, cell.bounds, photoPaint)

                if (showBorders) {
                    canvas.drawRect(cell.bounds, borderPaint)
                }
            }
        } else {
            // Draw placeholder dotted outlines when queue is empty
            val placeholderPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 2f
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                isAntiAlias = true
            }
            for (cell in grid.cells) {
                canvas.drawRect(cell.bounds, placeholderPaint)
            }
        }

        // 4. Interior Cut Lines & Intersection Crosshairs (Zero Paper Wastage)
        if (showScissorMarks || showBorders) {
            val solidLinePaint = Paint().apply {
                color = if (showBorders) borderColor else Color.argb(190, 160, 160, 160)
                strokeWidth = maxOf(1.2f, grid.sheetWidthPx * 0.0006f)
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val dashedPaint = Paint().apply {
                color = if (showBorders) borderColor else Color.argb(180, 150, 150, 150)
                strokeWidth = maxOf(1.5f, borderWidth * 0.75f)
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
                isAntiAlias = true
            }

            // Draw interior dividing cut lines between photos
            for (line in grid.cutLines) {
                canvas.drawLine(line.startX, line.startY, line.endX, line.endY, dashedPaint)
            }

            // Corner crosshairs at interior intersections
            val arm = maxOf(10f, grid.sheetWidthPx * 0.008f)
            for (ch in grid.crosshairs) {
                canvas.drawLine(ch.x - arm, ch.y, ch.x + arm, ch.y, solidLinePaint)
                canvas.drawLine(ch.x, ch.y - arm, ch.x, ch.y + arm, solidLinePaint)
            }
        }

        // 5. Scissor Icons (✂) along Interior Cut Line Endpoints
        if (showScissorMarks && grid.cutLines.isNotEmpty()) {
            val scissorSize = maxOf(20f, grid.sheetWidthPx * 0.014f)
            val scissorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 40, 40, 40)
                textSize = scissorSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            for (line in grid.cutLines) {
                if (line.startX == line.endX) {
                    // Vertical line: mark scissors at top and bottom ends
                    canvas.drawText("✂", line.startX, scissorSize * 1.1f, scissorPaint)
                    canvas.drawText("✂", line.startX, grid.sheetHeightPx - (scissorSize * 0.3f), scissorPaint)
                } else if (line.startY == line.endY) {
                    // Horizontal line: mark scissors at left and right ends
                    canvas.drawText("✂", scissorSize * 0.8f, line.startY + (scissorSize * 0.35f), scissorPaint)
                    canvas.drawText("✂", grid.sheetWidthPx - (scissorSize * 0.8f), line.startY + (scissorSize * 0.35f), scissorPaint)
                }
            }
        }

        // 6. Studio Margin Header / Metadata Imprint
        if (includeStudioWatermark && grid.marginYPx > 22f) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(130, 95, 100, 110)
                textSize = maxOf(13f, grid.sheetWidthPx * 0.0095f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }

            val mixInfo = if (photos.size > 1) " • Mixed Queue: ${photos.size} unique photos" else ""
            val metaText = "GemID Photo Studio • ${paperSize.title} (${grid.sheetWidthPx}×${grid.sheetHeightPx}px @ 300 DPI) • ${grid.activeCount} Photos$mixInfo"
            val textY = if (grid.marginYPx > 40f) (grid.marginYPx * 0.28f) else (grid.sheetHeightPx - 8f)
            canvas.drawText(metaText, grid.sheetWidthPx / 2f, textY, textPaint)
        }

        return@withContext sheetBitmap
    }

    /**
     * Fast downscaled bitmap for Compose live rendering without heavy memory usage.
     */
    fun createPreviewBitmap(masterSheet: Bitmap, targetWidth: Int = 1000): Bitmap {
        val aspect = masterSheet.width.toFloat() / masterSheet.height.toFloat()
        val targetHeight = maxOf(1, (targetWidth / aspect).toInt())
        return Bitmap.createScaledBitmap(masterSheet, targetWidth, targetHeight, true)
    }
}
