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

        // 3. Draw Photos into each Grid Cell, looping through photo queue
        if (photos.isNotEmpty()) {
            for (cell in grid.cells) {
                // Automatically loops through the photo list
                val photoToDraw = photos[cell.index % photos.size]
                val srcRect = Rect(0, 0, photoToDraw.width, photoToDraw.height)

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

        // 4. Dashed Crop Guidelines & Hairline Crop Lines
        if (showScissorMarks || showBorders) {
            val dashedPaint = Paint().apply {
                color = if (showBorders) borderColor else Color.argb(180, 160, 160, 160)
                strokeWidth = maxOf(1.5f, borderWidth * 0.75f)
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
                isAntiAlias = true
            }

            val solidLinePaint = Paint().apply {
                color = if (showBorders) borderColor else Color.argb(200, 140, 140, 140)
                strokeWidth = maxOf(1.2f, grid.sheetWidthPx * 0.0007f)
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            // Outer margin cutting ticks (extending to sheet edge)
            for (line in grid.cutLines) {
                canvas.drawLine(line.startX, line.startY, line.endX, line.endY, solidLinePaint)
            }

            // Dashed lines between photo columns
            for (col in 0 until grid.cols) {
                val left = grid.marginXPx + (col * (grid.photoWidthPx + grid.spacingXPx))
                val right = left + grid.photoWidthPx

                if (col < grid.cols - 1 && grid.spacingXPx > 0) {
                    val midX = right + (grid.spacingXPx / 2f)
                    canvas.drawLine(midX, grid.marginYPx, midX, grid.sheetHeightPx - grid.marginYPx, dashedPaint)
                }
            }

            // Dashed lines between photo rows
            for (row in 0 until grid.rows) {
                val top = grid.marginYPx + (row * (grid.photoHeightPx + grid.spacingYPx))
                val bottom = top + grid.photoHeightPx

                if (row < grid.rows - 1 && grid.spacingYPx > 0) {
                    val midY = bottom + (grid.spacingYPx / 2f)
                    canvas.drawLine(grid.marginXPx, midY, grid.sheetWidthPx - grid.marginXPx, midY, dashedPaint)
                }
            }

            // Corner crosshairs at intersections
            val arm = maxOf(10f, grid.sheetWidthPx * 0.008f)
            for (ch in grid.crosshairs) {
                canvas.drawLine(ch.x - arm, ch.y, ch.x + arm, ch.y, solidLinePaint)
                canvas.drawLine(ch.x, ch.y - arm, ch.x, ch.y + arm, solidLinePaint)
            }
        }

        // 5. Scissor Cutting Marks (✂) in Sheet Margins
        if (showScissorMarks) {
            val scissorSize = maxOf(22f, grid.sheetWidthPx * 0.016f)
            val scissorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 60, 60, 60) // Studio Dark Charcoal
                textSize = scissorSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            // Top & bottom margin scissors along column boundary lines
            val topScissorY = if (grid.marginYPx > scissorSize) grid.marginYPx * 0.55f else (scissorSize * 0.85f)
            val bottomScissorY = if (grid.marginYPx > scissorSize) {
                grid.sheetHeightPx - (grid.marginYPx * 0.35f)
            } else {
                grid.sheetHeightPx - 6f
            }

            for (col in 0 until grid.cols) {
                val cellLeft = grid.marginXPx + (col * (grid.photoWidthPx + grid.spacingXPx))
                val cellRight = cellLeft + grid.photoWidthPx

                canvas.drawText("✂", cellLeft, topScissorY, scissorPaint)
                canvas.drawText("✂", cellLeft, bottomScissorY, scissorPaint)

                canvas.drawText("✂", cellRight, topScissorY, scissorPaint)
                canvas.drawText("✂", cellRight, bottomScissorY, scissorPaint)
            }

            // Left & right margin scissors along row boundary lines
            val leftScissorX = if (grid.marginXPx > scissorSize) grid.marginXPx * 0.5f else (scissorSize * 0.6f)
            val rightScissorX = if (grid.marginXPx > scissorSize) {
                grid.sheetWidthPx - (grid.marginXPx * 0.5f)
            } else {
                grid.sheetWidthPx - (scissorSize * 0.6f)
            }

            val textOffset = scissorSize * 0.35f
            for (row in 0 until grid.rows) {
                val cellTop = grid.marginYPx + (row * (grid.photoHeightPx + grid.spacingYPx))
                val cellBottom = cellTop + grid.photoHeightPx

                canvas.drawText("✂", leftScissorX, cellTop + textOffset, scissorPaint)
                canvas.drawText("✂", leftScissorX, cellBottom + textOffset, scissorPaint)
                canvas.drawText("✂", rightScissorX, cellTop + textOffset, scissorPaint)
                canvas.drawText("✂", rightScissorX, cellBottom + textOffset, scissorPaint)
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
